(function($) {
	
	$.fn.extend( {
		
		// plugin name - specview
		specview: function(opts) {
			
			var defaults = {
					sequence: null,
					scanNum: null,
					fileName: null,
					charge: null,
					staticMods: [],
					variableMods: [],
					ntermMod: 0,
					ctermMod: 0,
					peaks: [],
					width: 750, 	// width of the ms/ms plot
					height: 450, 	// height of the ms/ms plot
					massError: 0.5, // mass tolerance for labeling peaks
			};
		
			var opts = $.extend(defaults, opts);
			
			return this.each(function() {
				
				options = opts;
				
				var input = new Peptide(options.sequence, options.staticMods, options.variableMods,
										options.ntermMod, options.ctermMod);
				
				container = $(this);
				initContainer(container, options);
				makeOptionsTable();
				showSequenceInfo();
				showSpecModInfo();
				
				massError = options.massError;
				
				createPlot(getDatasets()); // Initial Plot
				setupInteractions();
				
				makeIonTable();
			});
		}
	
	});

	var container;
	var options;
	var massError;
	var massErrorChanged = false;
	var massTypeChanged = false;
	var peakAssignmentTypeChanged = false;
	
	var plotOptions = {
    	series: {
            peaks: { show: true, lineWidth: 1, shadowSize: 0},
            shadowSize: 0
        },
        selection: { mode: "x" },
        grid: { show: true, hoverable: true, clickable: false, autoHighlight: false },
        xaxis: { tickLength: 5, tickColor: "#000" },
    	yaxis: { tickLength: 5, tickColor: "#000" }
	};
	
	var plot;
	var zoomRange; 				// for zooming
	var previousPoint = null; 	// for tooltips
	
	var ionSeries = {a: [], b: [], c: [], x:[], y: [], z: []};
	var ionSeriesMatch = {a: [], b: [], c: [], x: [], y: [], z: []};
	var ionSeriesLabels = {a: [], b: [], c: [], x: [], y: [], z: []};
	
	
	function round(number) {
		return number.toFixed(4);
	}
	
	function setMassError() {
   		var me = parseFloat(container.find("#massError").val());
		if(me != massError) {
			massError = me;
			massErrorChanged = true;
		}
		else {
			massErrorChanged = false;
		}
   	}
	
	function createPlot(datasets) {
    	if(!zoomRange) {
    		plot = $.plot(container.find("#msmsplot"), datasets,  plotOptions);
    	}
    	else {
    		plot = $.plot(container.find("#msmsplot"), datasets,
                      $.extend(true, {}, plotOptions, {
                          xaxis: { min: zoomRange.xaxis.from, max: zoomRange.xaxis.to }
             }));
    	}
    	// we have re-calculated and re-drawn everything..
    	massTypeChanged = false;
    	massErrorChanged = false;
    	peakAssignmentTypeChanged = false;
    }
	
	// -----------------------------------------------
	// SET UP INTERACTIVE ACTIONS
	// -----------------------------------------------
	function setupInteractions () {
		
		// CONNECT PLOTS FOR ZOOMING
	    container.find("#msmsplot").bind("plotselected", function (event, ranges) {
	    	
	    	zoomRange = ranges;
	    	createPlot(getDatasets());
	    });
	    
		// RESET ZOOM
		container.find("#resetZoom").click(function() {
			zoomRange = null;
			setMassError();
			createPlot(getDatasets());	
	   	});
		
		// UPDATE
		container.find("#update").click(function() {
			zoomRange = null; // zoom out fully
			setMassError();
			createPlot(getDatasets());
			makeIonTable();
	   	});
		
		// TOOLTIPS
		container.find("#msmsplot").bind("plothover", function (event, pos, item) {

	        if (container.find("#enableTooltip:checked").length > 0) {
	            if (item) {
	                if (previousPoint != item.datapoint) {
	                    previousPoint = item.datapoint;
	                    
	                    $("#msmstooltip").remove();
	                    var x = item.datapoint[0].toFixed(2),
	                        y = item.datapoint[1].toFixed(2);
	                    
	                    showTooltip(item.pageX, item.pageY,
	                                "m/z: " + x + "<br>intensity: " + y);
	                }
	            }
	            else {
	                $("#msmstooltip").remove();
	                previousPoint = null;            
	            }
	        }
	    });
		
		// SHOW / HIDE ION SERIES; UPDATE ON MASS TYPE CHANGE; PEAK ASSIGNMENT TYPE CHANGED
		var ionChoiceContainer = container.find("#ion_choice");
		ionChoiceContainer.find("input").click(plotAccordingToChoices);
		
	    container.find("input[name='massTypeOpt']").click(function() {
	    	massTypeChanged = true;
	    	plotAccordingToChoices();
	    });
	    container.find("input[name='peakAssignOpt']").click(function() {
	    	peakAssignmentTypeChanged = true;
	    	plotAccordingToChoices();
	    });
	    container.find("#deselectIonsLink").click(function() {
			ionChoiceContainer.find("input:checkbox:checked").each(function() {
				$(this).attr('checked', "");
			});
			
			plotAccordingToChoices();
		});
	    
	    
	    // MOVING THE ION TABLE
	    makeIonTableMovable();
	    
	    // CHANGING THE PLOT SIZE
	    makePlotResizable();
		
	}
	
	function plotAccordingToChoices() {
        var data = getDatasets();

		if (data.length > 0) {
            createPlot(data);
            makeIonTable();
        }
    }
	
	function showTooltip(x, y, contents) {
        $('<div id="msmstooltip">' + contents + '</div>').css( {
            position: 'absolute',
            display: 'none',
            top: y + 5,
            left: x + 5,
            border: '1px solid #fdd',
            padding: '2px',
            'background-color': '#F0E68C',
            opacity: 0.80
        }).appendTo("body").fadeIn(200);
    }
	
	function makePlotResizable() {
		
		container.find("#slider_width").slider({
			value:options.width,
			min: 100,
			max: 1500,
			step: 50,
			slide: function(event, ui) {
				var width = ui.value;
				//console.log(ui.value);
				options.width = width;
				container.find("#msmsplot").css({width: width});
				plotAccordingToChoices();
				container.find("#slider_width_val").text(width);
			}
		});
		
		container.find("#slider_height").slider({
			value:options.height,
			min: 100,
			max: 1000,
			step: 50,
			slide: function(event, ui) {
				var height = ui.value;
				//console.log(ui.value);
				options.height = height
				container.find("#msmsplot").css({height: height});
				plotAccordingToChoices();
				container.find("#slider_height_val").text(height);
			}
		});
	}
	

	// -----------------------------------------------
	// SELECTED DATASETS
	// -----------------------------------------------
	function getDatasets() {

		 // selected ions
		var selectedIonTypes = getSelectedIonTypes();
		
		calculateTheoreticalSeries(selectedIonTypes);
		var data = [{data: options.peaks, color: "#bbbbbb"}];
		
		var seriesMatches = getSeriesMatches(selectedIonTypes);
		for(var i = 0; i < seriesMatches.length; i += 1) {
			data.push(seriesMatches[i]);
		}
		return data;
	}
	
	//-----------------------------------------------
	// SELECTED ION TYPES
	// -----------------------------------------------
	function getSelectedIonTypes() {

		var ions = [];
		var charges = [];
		
		container.find("#ion_choice").find("input:checked").each(function () {
	        var key = $(this).attr("id");
	        var tokens = key.split("_");
	        ions.push(tokens[0]);
	        charges.push(tokens[1]);
	  	});
	    
	    var selected = [];
	    for(var i = 0; i < ions.length; i += 1) {
	    	selected.push(ion = Ion.get(ions[i], charges[i]));
	    }
	    
	    return selected;
	}
	
	function getSelectedNtermIons(selectedIonTypes) {
		var ntermIons = [];
		
		for(var i = 0; i < selectedIonTypes.length; i += 1) {
			var sion = selectedIonTypes[i];
			if(sion.type == "a" || sion.type == "b" || sion.type == "c") 
				ntermIons.push(sion);
		}
		ntermIons.sort(function(m,n) {
			if(m.type == n.type) {
				return (m.charge - n.charge);
			}
			else {
				return m.type - n.type;
			}
		});
		return ntermIons;
	}

	function getSelectedCtermIons(selectedIonTypes) {
		var ctermIons = [];
		
		for(var i = 0; i < selectedIonTypes.length; i += 1) {
			var sion = selectedIonTypes[i];
			if(sion.type == "x" || sion.type == "y" || sion.type == "z") 
				ctermIons.push(sion);
		}
		ctermIons.sort(function(m,n) {
			if(m.type == n.type) {
				return (m.charge - n.charge);
			}
			else {
				return m.type - n.type;
			}
		});
		return ctermIons;
	}
	
	// ---------------------------------------------------------
	// CALCUALTE THEORETICAL MASSES FOR THE SELECTED ION SERIES
	// ---------------------------------------------------------
	function calculateTheoreticalSeries(selectedIons) {

		if(selectedIons) {
		
			var todoIonSeries = [];
			var todoIonSeriesData = [];
			for(var i = 0; i < selectedIons.length; i += 1) {
				var sion = selectedIons[i];
				if(sion.type == "a") {
					if(!massTypeChanged && ionSeries.a[sion.charge])	continue; // already calculated
					else {
						todoIonSeries.push(sion);
						ionSeries.a[sion.charge] = [];
						todoIonSeriesData.push(ionSeries.a[sion.charge]);
					}
				}
				if(sion.type == "b") {
					if(!massTypeChanged && ionSeries.b[sion.charge])	continue; // already calculated
					else {
						todoIonSeries.push(sion);
						ionSeries.b[sion.charge] = [];
						todoIonSeriesData.push(ionSeries.b[sion.charge]);
					}
				}
				if(sion.type == "c") {
					if(!massTypeChanged && ionSeries.c[sion.charge])	continue; // already calculated
					else {
						todoIonSeries.push(sion);
						ionSeries.c[sion.charge] = [];
						todoIonSeriesData.push(ionSeries.c[sion.charge]);
					}
				}
				if(sion.type == "x") {
					if(!massTypeChanged && ionSeries.x[sion.charge])	continue; // already calculated
					else {
						todoIonSeries.push(sion);
						ionSeries.x[sion.charge] = [];
						todoIonSeriesData.push(ionSeries.x[sion.charge]);
					}
				}
				if(sion.type == "y") {
					if(!massTypeChanged && ionSeries.y[sion.charge])	continue; // already calculated
					else {
						todoIonSeries.push(sion);
						ionSeries.y[sion.charge] = [];
						todoIonSeriesData.push(ionSeries.y[sion.charge]);
					}
				}
				if(sion.type == "z") {
					if(!massTypeChanged && ionSeries.z[sion.charge])	continue; // already calculated
					else {
						todoIonSeries.push(sion);
						ionSeries.z[sion.charge] = [];
						todoIonSeriesData.push(ionSeries.z[sion.charge]);
					}
				}
			}

			if(options.sequence) {
				
				var massType = container.find("input[name='massTypeOpt']:checked").val();
				
				for(var i = 1; i < options.sequence.length; i += 1) {
					
					for(var j = 0; j < todoIonSeries.length; j += 1) {
						var tion = todoIonSeries[j];
						var ionSeriesData = todoIonSeriesData[j];
						if(tion.term == "n")
							ionSeriesData.push(sion = Ion.getSeriesIon(tion, options.sequence, i, massType));
						else if(tion.term == "c")
							ionSeriesData.unshift(sion = Ion.getSeriesIon(tion, options.sequence, i, massType));
					}
				}
			}
		}
	}

	
	// -----------------------------------------------
	// MATCH THEORETICAL MASSES WITH PEAKS IN THE SCAN
	// -----------------------------------------------
	function recalculate() {
		return (massErrorChanged || massTypeChanged || peakAssignmentTypeChanged);
	}

	function getSeriesMatches(selectedIonTypes) {
		
		var dataSeries = [];
		
		var peakAssignmentType = container.find("input[name='peakAssignOpt']:checked").val();
		
		for(var j = 0; j < selectedIonTypes.length; j += 1) {
		
			var ion = selectedIonTypes[j];
							
			
			if(ion.type == "a") {
				if(recalculate() || !ionSeriesMatch.a[ion.charge]) { // re-calculate only if mass error has changed OR
																		// matching peaks for this series have not been calculated
					// calculated matching peaks
					var adata = calculateMatchingPeaks(ionSeries.a[ion.charge], options.peaks, massError, peakAssignmentType);
					if(adata && adata.length > 0) {
						ionSeriesMatch.a[ion.charge] = adata[0];
						ionSeriesLabels.a[ion.charge] = adata[1];
					}
				}
				dataSeries.push({data: ionSeriesMatch.a[ion.charge], color: ion.color, labels: ionSeriesLabels.a[ion.charge]});
			}
			
			if(ion.type == "b") {
				if(recalculate() || !ionSeriesMatch.b[ion.charge]) { // re-calculate only if mass error has changed OR
																		// matching peaks for this series have not been calculated
					// calculated matching peaks
					var bdata = calculateMatchingPeaks(ionSeries.b[ion.charge], options.peaks, massError, peakAssignmentType);
					if(bdata && bdata.length > 0) {
						ionSeriesMatch.b[ion.charge] = bdata[0];
						ionSeriesLabels.b[ion.charge] = bdata[1];
					}
				}
				dataSeries.push({data: ionSeriesMatch.b[ion.charge], color: ion.color, labels: ionSeriesLabels.b[ion.charge]});
			}
			
			if(ion.type == "c") {
				if(recalculate() || !ionSeriesMatch.c[ion.charge]) { // re-calculate only if mass error has changed OR
																		// matching peaks for this series have not been calculated
					// calculated matching peaks
					var cdata = calculateMatchingPeaks(ionSeries.c[ion.charge], options.peaks, massError, peakAssignmentType);
					if(cdata && cdata.length > 0) {
						ionSeriesMatch.c[ion.charge] = cdata[0];
						ionSeriesLabels.c[ion.charge] = cdata[1];
					}
				}
				dataSeries.push({data: ionSeriesMatch.c[ion.charge], color: ion.color, labels: ionSeriesLabels.c[ion.charge]});
			}
			
			if(ion.type == "x") {
				if(recalculate() || !ionSeriesMatch.x[ion.charge]) { // re-calculate only if mass error has changed OR
																		// matching peaks for this series have not been calculated
					// calculated matching peaks
					var xdata = calculateMatchingPeaks(ionSeries.x[ion.charge], options.peaks, massError, peakAssignmentType);
					if(xdata && xdata.length > 0) {
						ionSeriesMatch.x[ion.charge] = xdata[0];
						ionSeriesLabels.x[ion.charge] = xdata[1];
					}
				}
				dataSeries.push({data: ionSeriesMatch.x[ion.charge], color: ion.color, labels: ionSeriesLabels.x[ion.charge]});
			}
			
			if(ion.type == "y") {
				if(recalculate() || !ionSeriesMatch.y[ion.charge]) { // re-calculate only if mass error has changed OR
																		// matching peaks for this series have not been calculated
					// calculated matching peaks
					var ydata = calculateMatchingPeaks(ionSeries.y[ion.charge], options.peaks, massError, peakAssignmentType);
					if(ydata && ydata.length > 0) {
						ionSeriesMatch.y[ion.charge] = ydata[0];
						ionSeriesLabels.y[ion.charge] = ydata[1];
					}
				}
				dataSeries.push({data: ionSeriesMatch.y[ion.charge], color: ion.color, labels: ionSeriesLabels.y[ion.charge]});
			}
			
			if(ion.type == "z") {
				if(recalculate() || !ionSeriesMatch.z[ion.charge]) { // re-calculate only if mass error has changed OR
																		// matching peaks for this series have not been calculated
					// calculated matching peaks
					var zdata = calculateMatchingPeaks(ionSeries.z[ion.charge], options.peaks, massError, peakAssignmentType);
					if(zdata && zdata.length > 0) {
						ionSeriesMatch.z[ion.charge] = zdata[0];
						ionSeriesLabels.z[ion.charge] = zdata[1];
					}
				}
				dataSeries.push({data: ionSeriesMatch.z[ion.charge], color: ion.color, labels: ionSeriesLabels.z[ion.charge]});
			}
		}
		return dataSeries;
	}

	function calculateMatchingPeaks(ionSeries, allPeaks, massTolerance, peakAssignmentType) {
		
		var bestPeak;
		var peakIndex = 0;
		
		var matchData = [];
		matchData[0] = []; // peaks
		matchData[1] = []; // labels;
		
		for(var i = 0; i < ionSeries.length; i += 1) {
			
			bestPeak = null; // reset
			var sion = ionSeries[i];
			sion.match = false; // reset;
			var bestDistance;
			
			for(var j = peakIndex; j < allPeaks.length; j += 1) {
				
				var peak = allPeaks[j];
				
				// peak is before the current ion we are looking at
				if(peak[0] < sion.mz - massTolerance)
					continue;
					
				// peak is beyond the current ion we are looking at
				if(peak[0] > sion.mz + massTolerance) {
				
					// if we found a matching peak for the current ion, save it
					if(bestPeak) {
						//console.log("found match "+sion.label+", "+sion.mz+";  peak: "+bestPeak[0]);
						matchData[0].push([bestPeak[0], bestPeak[1]]);
						matchData[1].push(sion.label);
						sion.match = true;
					}
					peakIndex = j;
					break;
				}
					
				// peak is within +/- massTolerance of the current ion we are looking at
				
				// if this is the first peak in the range
				if(!bestPeak) {
					//console.log("found a peak in range, "+peak.mz);
					bestPeak = peak;
					bestDistance = Math.abs(sion.mz - peak[0]);
					continue;
				}
				
				// if peak assignment method is Most Intense
				if(peakAssignmentType == "intense") {
					if(peak[1] > bestPeak[1]) {
						bestPeak = peak;
						continue;
					}
				}
				
				// if peak assignment method is Closest Peak
				if(peakAssignmentType == "close") {
					var dist = Math.abs(sion.mz - peak[0]);
					if(!bestDistance || dist < bestDistance) {
						bestPeak = peak;
						bestDistance = dist;
					}
				}
			}
		}
		return matchData;
	}
	
	
	// -----------------------------------------------
	// INITIALIZE THE CONTAINER
	// -----------------------------------------------
	function initContainer(container, options) {
		
		container.addClass("mainContainer");
		
		var parentTable = '<table cellpadding="0" cellspacing="5"> ';
		parentTable += '<tbody> ';
		parentTable += '<tr> ';
		
		// Header
		parentTable += '<td colspan="3" style="background-color:#939CB0; color:white; padding:0px;"> ';
		parentTable += '<div align="center" style="width:100%; height:15px;"> ';
		parentTable += '</div> ';
		parentTable += '</td> ';
		parentTable += '</tr> ';
	
		// options table
		parentTable += '<tr> ';
		parentTable += '<td rowspan="3" valign="top" id="optionsTable" > ';
		parentTable += '</td> ';
		
		// placeholder for sequence, m/z, scan number etc
		parentTable += '<td style="background-color: white; padding:5px; border:1px dotted #cccccc;" valign="bottom" align="center"> '; 
		parentTable += '<div id="seqinfo" style="width:100%;"></div> ';
		parentTable += '</td> ';
		
		// placeholder for the ion table
		parentTable += '<td rowspan="3" style="padding:5px;" valign="top" id="ionTableLoc1"> ';
		parentTable += '<div id="ionTableDiv"><span id="moveIonTable" style="color: sienna; text-decoration:underline;">[Move]</span></div> ';
		parentTable += '</td> ';
		parentTable += '</tr> ';
		
		// placeholders for the plots
		parentTable += '<tr> ';
		parentTable += '<td style="background-color: white; padding:5px; border:1px dotted #cccccc;" valign="middle" align="center"> '; 
		parentTable += '<div id="msmsplot" style="width:'+options.width+'px;height:'+options.height+'px;"></div> ';
		parentTable += '</td> ';
		parentTable += '</tr> ';
		
		// placeholder for file name, scan number, modifications etc.
		parentTable += '<tr> ';
		parentTable += '<td style="background-color: white; padding:5px; border:1px dotted #cccccc;" valign="bottom" align="center"> '; 
		parentTable += '<div id="specmodinfo" style="width:100%;"></div> ';
		parentTable += '</td> ';
		parentTable += '</tr> ';
		
		// Placeholder for moving ion table
		parentTable += '<tr> ';
		parentTable += '<td colspan="3" style="padding:2px;" valign="top" align="center" id="ionTableLoc2"> ';
		parentTable += '</td> ';
		parentTable += '</tr> ';
		
		// Footer
		parentTable += '<tr> ';
		parentTable += '<td colspan="3" style="background-color:#939CB0; color:white; padding:2px;"> ';
		parentTable += '<div align="center" style="width:100%;font-size:10pt;"> ';
		parentTable += '<b>Spectrum Viewer</b> by vsharma@uw.edu';
		parentTable += '</div> ';
		parentTable += '</td> ';
		parentTable += '</tr> ';
		
		parentTable += '</tbody> ';
		parentTable += '</table> ';
		
		container.append(parentTable);
	}
	
	function makeIonTableMovable() {
		
		container.find("#moveIonTable").hover(
			function(){
		         $(this).css({cursor:'pointer'}); //mouseover
		    }
		);
		
		container.find("#moveIonTable").click(function() {
			var ionTableDiv = container.find("#ionTableDiv");
			if(ionTableDiv.is(".moved")) {
				ionTableDiv.removeClass("moved");
				ionTableDiv.detach();
				container.find("#ionTableLoc1").append(ionTableDiv);
			}
			else {
				ionTableDiv.addClass("moved");
				ionTableDiv.detach();
				container.find("#ionTableLoc2").append(ionTableDiv);
			}
		});
	}
	
	
	//---------------------------------------------------------
	// ION TABLE
	//---------------------------------------------------------
	function makeIonTable() {
		
	 	// selected ions
		var selectedIonTypes = getSelectedIonTypes();
		var ntermIons = getSelectedNtermIons(selectedIonTypes);
		var ctermIons = getSelectedCtermIons(selectedIonTypes);
		
		var myTable = '' ;
		myTable += '<table id="ionTable" cellpadding="2">' ;
		myTable +=  "<thead>" ;
		myTable +=   "<tr>";
		// nterm ions
		for(var i = 0; i < ntermIons.length; i += 1) {
			myTable +=    "<th>" +ntermIons[i].label+  "</th>";   
		}
		myTable +=    "<th>" +"#"+  "</th>"; 
		myTable +=    "<th>" +"Seq"+  "</th>"; 
		myTable +=    "<th>" +"#"+  "</th>"; 
		// cterm ions
		for(var i = 0; i < ctermIons.length; i += 1) {
			myTable +=    "<th>" +ctermIons[i].label+  "</th>"; 
		}
		myTable +=   "</tr>" ;
		myTable +=  "</thead>" ;
		
		myTable +=  "<tbody>" ;
		for(var i = 0; i < options.sequence.length; i += 1) {
			myTable +=   "<tr>";
			
			// nterm ions
			for(var n = 0; n < ntermIons.length; n += 1) {
				if(i < options.sequence.length - 1) {
					var seriesData = getCalculatedSeries(ntermIons[n]);
					var cls = "";
					var style = "";
					if(seriesData[i].match) {
						cls="matchIon";
						style="style='background-color:"+Ion.getSeriesColor(ntermIons[n])+";'";
					}
					myTable +=    "<td class='"+cls+"' "+style+" >" +round(seriesData[i].mz)+  "</td>";  
				}
				else {
					myTable +=    "<td>" +"&nbsp;"+  "</td>"; 
				} 
			}
			
			myTable += "<td class='numCell'>"+(i+1)+"</td>";
			if(Peptide.varMods[i+1])
				myTable += "<td class='seq modified'>"+options.sequence[i]+"</td>";
			else
				myTable += "<td class='seq'>"+options.sequence[i]+"</td>";
			myTable += "<td class='numCell'>"+(options.sequence.length - i)+"</td>";
			
			// cterm ions
			for(var c = 0; c < ctermIons.length; c += 1) {
				if(i > 0) {
					var seriesData = getCalculatedSeries(ctermIons[c]);
					var idx = options.sequence.length - i - 1;
					var cls = "";
					var style = "";
					if(seriesData[idx].match) {
						cls="matchIon";
						style="style='background-color:"+Ion.getSeriesColor(ctermIons[c])+";'";
					}
					myTable +=    "<td class='"+cls+"' "+style+" >" +round(seriesData[idx].mz)+  "</td>";  
				}
				else {
					myTable +=    "<td>" +"&nbsp;"+  "</td>"; 
				} 
			}
			
		}
		myTable +=   "</tr>" ;
		
		myTable += "</tbody>";
		myTable += "</table>";
		
		//alert(myTable);
		container.find("#ionTable").remove();
		container.find("#ionTableDiv").prepend(myTable); // add as the first child
	}
	
	function getCalculatedSeries(ion) {
		if(ion.type == "a")
			return ionSeries.a[ion.charge];
		if(ion.type == "b")
			return ionSeries.b[ion.charge];
		if(ion.type == "c")
			return ionSeries.c[ion.charge];
		if(ion.type == "x")
			return ionSeries.x[ion.charge];
		if(ion.type == "y")
			return ionSeries.y[ion.charge];
		if(ion.type == "z")
			return ionSeries.z[ion.charge];
	}
	
	
	//---------------------------------------------------------
	// SEQUENCE INFO
	//---------------------------------------------------------
	function showSequenceInfo () {
		
		var specinfo = '';
		if(options.sequence) {
			
			specinfo += '<div>';
			specinfo += '<span style="font-weight:bold; color:#8B0000;">'+getModifiedSequence()+'</span>';
			var mass = Peptide.getSeqMassMono(options.sequence, options.sequence.length, "n");
			// add C-terminal OH
			mass = mass + Ion.MASS_O + Ion.MASS_H;
			var mz;
			if(options.charge) {
				mz = Ion.getMz(mass, options.charge);
			}
			mass = mass + Ion.MASS_PROTON;
			specinfo += ', MH+ '+mass.toFixed(4);
			if(mz) 
				specinfo += ', m/z '+mz.toFixed(4);
			specinfo += '</div>';
			
		}
		
		container.find("#seqinfo").append(specinfo);
	}
	
	function getModifiedSequence() {
		
		var modSeq = '';
		for(var i = 0; i < options.sequence.length; i += 1) {
			
			if(Peptide.varMods[i+1])
				modSeq += '<span style="background-color:yellow;padding:1px;border:1px dotted #CFCFCF;">'+options.sequence[i]+"</span>";
			else
				modSeq += options.sequence[i];
		}
		
		return modSeq;
	}
	
	//---------------------------------------------------------
	// SPECTRUM INFO
	//---------------------------------------------------------
	function showSpecModInfo () {
		
		var specinfo = '';
		if(options.sequence) {
			
			specinfo += '<div>';
			if(options.ntermMod || options.ntermMod > 0) {
				specinfo += 'Add to N-term: <b>'+options.ntermMod+'</b>';
			}
			if(options.ctermMod || options.ctermMod > 0) {
				specinfo += 'Add to C-term: <b>'+options.ctermMod+'</b>';
			}
			specinfo += '</div>';
			
			if(options.staticMods && options.staticMods.length > 0) {
				specinfo += '<div style="margin-top:10px;>';
				specinfo += 'Static Modifications: ';
				for(var i = 0; i < options.staticMods.length; i += 1) {
					var mod = options.staticMods[i];
					if(i > 0) specinfo += ', ';
					specinfo += mod.aa.code+": "+mod.modMass;
				}
				specinfo += '</div>';
			}
			
			if(options.variableMods && options.variableMods.length > 0) {
				
				var modChars = [];
				var uniqvarmods = [];
				for(var i = 0; i < options.variableMods.length; i += 1) {
					var mod = options.variableMods[i];
					if(modChars[mod.aa.code])
						continue;
					modChars[mod.aa.code] = 1;
					uniqvarmods.push(mod);
				}  
				
				specinfo += '<div style="margin-top:10px;">';
				specinfo += 'Variable Modifications: ';
				for(var i = 0; i < uniqvarmods.length; i += 1) {
					var mod = uniqvarmods[i];
					if(i > 0) specinfo += ', ';
					specinfo += "<b>"+mod.aa.code+" ("+mod.modMass+")</b>";
				}
				specinfo += '</div>';
			}
			
			if(options.fileName || options.scanNum) {
				specinfo += '<div style="margin-top:10px;">';
				if(options.fileName) {
					specinfo += 'File: '+options.fileName;
				}
				if(options.scanNum) {
					specinfo += ', Scan: '+options.scanNum;
				}	
				if(options.charge) {
					specinfo += ', Charge: '+options.charge;
				}
				specinfo += '</div>';
			}
		}
		
		container.find("#specmodinfo").append(specinfo);
	}
	
	
	//---------------------------------------------------------
	// OPTIONS TABLE
	//---------------------------------------------------------
	function makeOptionsTable() {
		
		var myTable = '';
		myTable += '<table cellpadding="2" cellspacing="2"> ';
		myTable += '<tbody> ';
		
		// Ions
		myTable += '<tr><td class="optionCell"> ';
		
		myTable += '<b>Ions:</b> ';
		myTable += '<div id="ion_choice" style="margin-bottom: 10px"> ';
		myTable += '<!-- a ions --> ';
		myTable += '<nobr> ';
		myTable += '<b>a</b> ';
		myTable += '<input type="checkbox" value="1" id="a_1"/>1<sup>+</sup> ';
		myTable += '<input type="checkbox" value="2" id="a_2"/>2<sup>+</sup> ';
		myTable += '<input type="checkbox" value="3" id="a_3"/>3<sup>+</sup> ';
		myTable += '</nobr> ';
		myTable += '<br/> ';
		myTable += '<!-- b ions --> ';
		myTable += '<nobr> ';
		myTable += '<b>b</b> ';
		myTable += '<input type="checkbox" value="1" id="b_1" checked="checked"/>1<sup>+</sup> ';
		myTable += '<input type="checkbox" value="2" id="b_2"/>2<sup>+</sup> ';
		myTable += '<input type="checkbox" value="3" id="b_3"/>3<sup>+</sup> ';
		myTable += '</nobr> ';
		myTable += '<br/> ';
		myTable += '<!-- c ions --> ';
		myTable += '<nobr> ';
		myTable += '<b>c</b> ';
		myTable += '<input type="checkbox" value="1" id="c_1"/>1<sup>+</sup> ';
		myTable += '<input type="checkbox" value="2" id="c_2"/>2<sup>+</sup> ';
		myTable += '<input type="checkbox" value="3" id="c_3"/>3<sup>+</sup> ';
		myTable += '</nobr> ';
		myTable += '<br/> ';
		myTable += '<!-- x ions --> ';
		myTable += '<nobr> ';
		myTable += '<b>x</b> ';
		myTable += '<input type="checkbox" value="1" id="x_1"/>1<sup>+</sup> ';
		myTable += '<input type="checkbox" value="2" id="x_2"/>2<sup>+</sup> ';
		myTable += '<input type="checkbox" value="3" id="x_3"/>3<sup>+</sup> ';
		myTable += '</nobr> ';
		myTable += '<br/> ';
		myTable += '<!-- y ions --> ';
		myTable += '<nobr> ';
		myTable += '<b>y</b> ';
		myTable += '<input type="checkbox" value="1" id="y_1" checked="checked"/>1<sup>+</sup> ';
		myTable += '<input type="checkbox" value="2" id="y_2"/>2<sup>+</sup> ';
		myTable += '<input type="checkbox" value="3" id="y_3"/>3<sup>+</sup> ';
		myTable += '</nobr> ';
		myTable += '<br/> ';
		myTable += '<!-- z ions --> ';
		myTable += '<nobr> ';
		myTable += '<b>z</b> ';
		myTable += '<input type="checkbox" value="1" id="z_1"/>1<sup>+</sup> ';
		myTable += '<input type="checkbox" value="2" id="z_2"/>2<sup>+</sup> ';
		myTable += '<input type="checkbox" value="3" id="z_3"/>3<sup>+</sup> ';
		myTable += '</nobr> ';
		myTable += '<br/> ';
		myTable += '<span id="deselectIonsLink" style="font-size:8pt;text-decoration: underline; color:sienna;cursor:pointer;">[Deselect All]</span> ';
		myTable += '</div> ';
		
		myTable += '<b>Neutral Loss:</b> ';
		myTable += '<div id="nl_choice"> ';
		myTable += '<nobr> H<sub>2</sub>O ';
		myTable += '<input type="checkbox" value="h2o" id="h2o"/> ';
		myTable += '</nobr> ';
		myTable += '<nobr> NH<sub>3</sub> ';
		myTable += '<input type="checkbox" value="nh3" id="nh3"/> ';
		myTable += '</nobr> ';
		myTable += '</div> ';
		
		myTable += '</td> </tr> ';
		
		// mass type, mass tolerance etc.
		myTable += '<tr><td class="optionCell"> ';
		myTable += '<div> Mass Type:<br/> ';
		myTable += '<nobr> ';
		myTable += '<input type="radio" name="massTypeOpt" value="mono" checked="checked"/><b>Mono</b> ';
		myTable += '<input type="radio" name="massTypeOpt" value="avg"/><b>Avg</b> ';
		myTable += '</nobr> ';
		myTable += '</div> ';
		myTable += '<div style="margin-top:10px;"> ';
		myTable += '<nobr>Mass Tol: <input id="massError" type="text" value="'+options.massError+'" size="4"/></nobr> ';
		myTable += '</div> ';
		myTable += '<div style="margin-top:10px;" align="center"> ';
		myTable += '<input id="update" type="button" value="Update"/> ';
		myTable += '</div> ';
		myTable += '</td> </tr> ';
		
		// peak assignment method
		myTable += '<tr><td class="optionCell"> ';
		myTable+= '<div> Peak Assignment:<br/> ';
		myTable+= '<input type="radio" name="peakAssignOpt" value="intense" checked="checked"/><b>Most Intense</b><br/> ';
		myTable+= '<input type="radio" name="peakAssignOpt" value="close"/><b>Nearest Match</b> ';
		myTable+= '</div> ';
		myTable += '</td> </tr> ';
		
		// tooltip option
		myTable += '<tr><td class="optionCell"> ';
		myTable += '<input id="enableTooltip" type="checkbox">Enable tooltip ';
		myTable += '<br><br>';
		
		// reset zoom option
		myTable += '<div style="width:100%; font-size:8pt; margin-top:5px; color:sienna;">Click and drag <br> in the plot to zoom</div> ';
		myTable += '<input id="resetZoom" type="button" value="Zoom Out" /> ';
		myTable += '</td> </tr> ';
		
		
		// sliders to change plot size
		myTable += '<tr><td class="optionCell"> ';
		myTable += '<div>Width: <span id="slider_width_val">'+options.width+'</span></div> '
		myTable += '<div id="slider_width" style="margin-bottom:15px;"></div> ';
		myTable += '<div>Height: <span id="slider_height_val">'+options.height+'</span></div> '
		myTable += '<div id="slider_height"></div> ';
		myTable += '</td> </tr> ';
		
		
		myTable += '</tbody>';
		myTable += '</table>';
		
		container.find("#optionsTable").append(myTable);
	}
	
	
})(jQuery);