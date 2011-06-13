package org.yeastrc.ms.service.database.fasta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.yeastrc.ms.domain.general.EnzymeRule;
import org.yeastrc.ms.domain.general.MsEnzyme;
import org.yeastrc.nrseq.domain.NrDbProtein;

public class PeptideProteinMatchingServiceTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetPeptideProteinMatch4() throws PeptideProteinMatchingServiceException {
    	
    	String proteinSequence = "PNWVKTYIKFLQNSNLGGIIPTVNGKPVRQITDDELTFLYNTFQIFAPSQFLPTWVKDILSVDYTDIMKILSKSIEK*MQSDT*QEANDIVTLANLQYNGSTPADAFETKVTNIIDR";
        String peptide = "MQSDTQEANDIVTLANLQYNGSTPADAFETK";
        
        MsEnzyme enzyme = new MsEnzyme() {
            @Override
            public int getId() {
                return 0;
            }
            @Override
            public String getCut() {
                return "KR";
            }
            @Override
            public String getDescription() {
                return null;
            }
            @Override
            public String getName() {
                return "Trypsin_K";
            }
            @Override
            public String getNocut() {
                return "P";
            }

            @Override
            public Sense getSense() {
                return Sense.CTERM;
            }};
            
        EnzymeRule rule = new EnzymeRule(enzyme);
        List<EnzymeRule> rules = new ArrayList<EnzymeRule>(1);
        rules.add(rule);
        int minEnzymaticTermini = 1;
        
        PeptideProteinMatchingService service = new PeptideProteinMatchingService();
        service.setEnzymeRules(rules);
        service.setNumEnzymaticTermini(minEnzymaticTermini);
        service.setDoItoLSubstitution(false);
        service.setRemoveAsterisks(false);
        
        NrDbProtein dbProt = new NrDbProtein();
        dbProt.setAccessionString("YKR094C");
        dbProt.setDatabaseId(194);
        dbProt.setProteinId(532712);
        
        // We are not removing asterisks. We should NOT find a match
        service.setRemoveAsterisks(false); 
        PeptideProteinMatch match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNull(match);
        
        // We are removing asterisks. We should find a match with two enzymatic termini
        service.setRemoveAsterisks(true);
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(2, match.getNumEnzymaticTermini());
        
        // remove asterisk from the middle of the peptide match
        proteinSequence = "PNWVKTYIKFLQNSNLGGIIPTVNGKPVRQITDDELTFLYNTFQIFAPSQFLPTWVKDILSVDYTDIMKILSKSIEK*MQSDTQEANDIVTLANLQYNGSTPADAFETKVTNIIDR";
        service.setRemoveAsterisks(true); // all asterisks will be removed before finding a match
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(2, match.getNumEnzymaticTermini());
        
        
        // asterisks will NOT be removed before finding a match
        service.setRemoveAsterisks(false); 
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(2, match.getNumEnzymaticTermini()); // we should still get NET = 2 since the '*' will be treated a protein start.
        
        // change the peptide
        peptide = "MQSDTQEANDIVTLANLQYNGSTPADAFET";
        service.setRemoveAsterisks(false); 
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(1, match.getNumEnzymaticTermini()); // we should get NET = 1 since we've removed the cterminal 'K'.
        
        // replace 'K' before '*' with a 'T'
        proteinSequence = "PNWVKTYIKFLQNSNLGGIIPTVNGKPVRQITDDELTFLYNTFQIFAPSQFLPTWVKDILSVDYTDIMKILSKSIET*MQSDTQEANDIVTLANLQYNGSTPADAFETKVTNIIDR";
        service.setRemoveAsterisks(true); // all asterisks will be removed before finding a match
        service.setNumEnzymaticTermini(0);
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(0, match.getNumEnzymaticTermini()); // we should get NET = 0
        
        service.setRemoveAsterisks(false); // all asterisks will NOT be removed before finding a match
        service.setNumEnzymaticTermini(0);
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(1, match.getNumEnzymaticTermini()); // we should get NET = 1 due to '*' being treated as protein start
        
        // Add '*' after ...FET
        proteinSequence = "PNWVKTYIKFLQNSNLGGIIPTVNGKPVRQITDDELTFLYNTFQIFAPSQFLPTWVKDILSVDYTDIMKILSKSIET*MQSDTQEANDIVTLANLQYNGSTPADAFET*KVTNIIDR";
        service.setRemoveAsterisks(true); // all asterisks will be removed before finding a match
        service.setNumEnzymaticTermini(0);
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(0, match.getNumEnzymaticTermini()); // we should get NET = 0
        
        service.setRemoveAsterisks(false); // all asterisks will NOT be removed before finding a match
        service.setNumEnzymaticTermini(0);
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(2, match.getNumEnzymaticTermini()); // we should get NET = 1 due to the two '*' being treated as protein start end
        
    }
    
    
    public void testGetPeptideProteinMatch2() throws PeptideProteinMatchingServiceException {
    	
		String proteinSequence = "MESQQLSNYPNISHGSACASVTSKEVHTNQDPLDVSASKIQEYDKASTKANSQQTTTPASSAVPENLHHASPQPASVPPPQNGPYPQQCMMTQNQANPSGWSFYGHPSMIPYTPYQMSPMYFPPGPQSQFPQYPSSVGTPLSTPSPESGNTFTDSSSADSDMTSTKKYVRPPPMLTSPNDFPNWVKTYIKFLQNSNLGGIIPTVNGKPVRQITDDELTFLYNTFQIFAPSQFLPTWVKDILSVDYTDIMKILSKSIEKMQSDTQEANDIVTLANLQYNGSTPADAFETKVTNIIDRLNNNGIHINNKVACQLIMRGLSGEYKFLRYTRHRHLNMTVAELFLDIHAIYEEQQGSRNSKPNYRRNPSDEKNDSRSYTNTTKPKVIARNPQKTNNSKSKTARAHNVSTSNNSPSTDNDSISKSTTEPIQLNNKHDLHLGQKLTESTVNHTNHSDDELPGHLLLDSGASRTLIRSAHHIHSASSNPDINVVDAQKRNIPINAIGDLQFHFQDNTKTSIKVLHTPNIAYDLLSLNELAAVDITACFTKNVLERSDGTVLAPIVKYGDFYWVSKKYLLPSNISVPTINNVHTSESTRKYPYPFIHRMLAHANAQTIRYSLKNNTITYFNESDVDWSSAIDYQCPDCLIGKSTKHRHIKGSRLKYQNSYEPFQYLHTDIFGPVHNLPKSAPSYFISFTDETTKFRWVYPLHDRREDSILDVFTTILAFIKNQFQASVLVIQMDRGSEYTNRTLHKFLEKNGITPCYTTTADSRAHGVAERLNRTLLDDCRTQLQCSGLPNHLWFSAIEFSTIVRNSLASPKSKKSARQHAGLAGLDISTLLPFGQPVIVNDHNPNSKIHPRGIPGYALHPSRNSYGYIIYLPSLKKTVDTTNYVILQGKESRLDQFNYDALTFDEDLNRLTASYQSFIASNEIQQSDDLNIESDHDFQSDIELHPEQPRNVLSKAVSPTDSTPPSTHTEDSKRVSKTNIRAPREVDPNISESNILPSKKRSSTPQISNIESTGSGGMHKLNVPLLAPMSQSNTHESSHASKSKDFRHSDSYSENETNHTNVPISSTGGTNNKTVPQISDQETEKRIIHRSPSIDASPPENNSSHNIVPIKTPTTVSEQNTEESIIADLPLPDLPPESPTEFPDPFKELPPINSHQTNSSLGGIGDSNAYTTINSKKRSLEDNETEIKVSRDTWNTKNMRSLEPPRSKKRIHLIAAVKAVKSIKPIRTTLRYDEAITYNKDIKEKEKYIEAYHKEVNQLLKMNTWDTDKYYDRKEIDPKRVINSMFIFNRKRDGTHKARFVARGDIQHPDTYDSGMQSNTVHHYALMTSLSLALDNNYYITQLDISSAYLYADIKEELYIRPPPHLGMNDKLIRLKKSLYGLKQSGANWYETIKSYLIKQCGMEEVRGWSCVFKNSQVTICLFVDDMILFSKDLNANKKIITTLKKQYDTKIINLGESDNEIQYDILGLEIKYQRGKYMKLGMENSLTEKIPKLNVPLNPKGRKLSAPGQPGLYIDQDELEIDEDEYKEKVHEMQKLIGLASYVGYKFRFDLLYYINTLAQHILFPSRQVLDMTYELIQFMWDTRDKQLIWHKNKPTEPDNKLVAISDASYGNQPYYKSQIGNIYLLNGKVIGGKSTKASLTCTSTTEAEIHAISESVPLLNNLSHLVQELNKKPITKGLLTDSKSTISIIISNNEEKFRNRFFGTKAMRLRDEVSGNHLHVCYIETKKNIADVMTKPLPIKTFKLLTNKWIH";
        int minEnzymaticTermini = 1;
        String peptide = "MQSDTQEANDIVTLANLQYNGSTPADAFETK";
        
        MsEnzyme enzyme = new MsEnzyme() {
            @Override
            public int getId() {
                return 0;
            }
            @Override
            public String getCut() {
                return "KR";
            }
            @Override
            public String getDescription() {
                return null;
            }
            @Override
            public String getName() {
                return "Trypsin_K";
            }
            @Override
            public String getNocut() {
                return "P";
            }

            @Override
            public Sense getSense() {
                return Sense.CTERM;
            }};
            
        EnzymeRule rule = new EnzymeRule(enzyme);
        List<EnzymeRule> rules = new ArrayList<EnzymeRule>(1);
        rules.add(rule);
        
        PeptideProteinMatchingService service = new PeptideProteinMatchingService(194);
        service.setEnzymeRules(rules);
        service.setNumEnzymaticTermini(minEnzymaticTermini);
        
        List<PeptideProteinMatch> matches = service.getMatchingProteins(peptide);
        Set<String> accessions = new HashSet<String>();
        for(PeptideProteinMatch match: matches) {
        	accessions.add(match.getProtein().getAccessionString());
        }
        
        //System.out.println(matches.size());
        assertEquals(53, matches.size());
        assertTrue(accessions.contains("YML045W-A"));
        
        NrDbProtein dbProt = new NrDbProtein();
        dbProt.setAccessionString("YKR094C");
        dbProt.setDatabaseId(194);
        dbProt.setProteinId(532712);
        
        PeptideProteinMatch match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        
        assertNotNull(match);
        assertEquals(2, match.getNumEnzymaticTermini());
        
        peptide = "MQSDTQEANDLVTIANLQYNGSTPADAFETK"; // switch L and I
        
        service.setDoItoLSubstitution(false);
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNull(match);
        
        service.setDoItoLSubstitution(true);
        match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
        assertNotNull(match);
        assertEquals(2, match.getNumEnzymaticTermini());
        
	}

    // NOTE: COMMENTED OUT BECAUSE CREATING SUFFIX MAP TAKES A LONG TIME
//    public void testGetPeptideProteinMatch3() throws PeptideProteinMatchingServiceException {
//    	
//		String proteinSequence = "MQIFVKTLTGKTITLEVESSDTIDNVKSKIQDKEGIPPDQQRLIFAGKQLEDGRTLSDYNIQKESTLHLVLRLRGGIIEPSLKALASKYNCDKSVCRKCYARLPPRATNCRKRKCGHTNQLRPKKKLK";
//        int minEnzymaticTermini = 1;
//        String peptide = "IQDKEGIPPDQQR";
//        
//        MsEnzyme enzyme = new MsEnzyme() {
//            @Override
//            public int getId() {
//                return 0;
//            }
//            @Override
//            public String getCut() {
//                return "KR";
//            }
//            @Override
//            public String getDescription() {
//                return null;
//            }
//            @Override
//            public String getName() {
//                return "Trypsin_K";
//            }
//            @Override
//            public String getNocut() {
//                return "P";
//            }
//
//            @Override
//            public Sense getSense() {
//                return Sense.CTERM;
//            }};
//            
//        EnzymeRule rule = new EnzymeRule(enzyme);
//        List<EnzymeRule> rules = new ArrayList<EnzymeRule>(1);
//        rules.add(rule);
//        
//        NrDbProtein dbProt = new NrDbProtein();
//        dbProt.setAccessionString("YKR094C");
//        dbProt.setDatabaseId(194);
//        dbProt.setProteinId(531326);
//        
//        PeptideProteinMatchingService service = new PeptideProteinMatchingService(194);
//        service.setEnzymeRules(rules);
//        service.setNumEnzymaticTermini(minEnzymaticTermini);
//        
//        List<PeptideProteinMatch> matches = service.getMatchingProteins(peptide);
//        Set<String> accessions = new HashSet<String>();
//        for(PeptideProteinMatch match: matches) {
//        	accessions.add(match.getProtein().getAccessionString());
//        }
//        assertTrue(accessions.contains("YKR094C"));
//        assertTrue(accessions.contains("YLL039C"));
//        assertTrue(accessions.contains("YIL148W"));
//        assertTrue(accessions.contains("YLR167W"));
//        
//        PeptideProteinMatch match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
//        
//        assertNotNull(match);
//        assertEquals(2, match.getNumEnzymaticTermini());
//	}
    
    // NOTE: COMMENTED OUT BECAUSE CREATING SUFFIX MAP TAKES A LONG TIME
//	public void testGetPeptideProteinMatch1() throws PeptideProteinMatchingServiceException {
//		String proteinSequence = "MLPSWKAFKAHNILRILTRFQSTKIPDAVIGIDLGTTNSAVAIMEGKVPRIIENAEGSRTTPSVVAFTKDGERLVGEPAKRQSVINSENTLFATKRLIGRRFEDAEVQRDINQVPFKIVKHSNGDAWVEARNRTYSPAQIGGFILNKMKETAEAYLAKSVKNAVVTVPAYFNDAQRQATKDAGQIIGLNVLRVVNEPTAAALAYGLDKSEPKVIAVFDLGGGTFDISILDIDNGIFEVKSTNGDTHLGGEDFDIYLLQEIISHFKKETGIDLSNDRMAVQRIREAAEKAKIELSSTLSTEINLPFITADAAGPKHIRMPFSRVQLENITAPLIDRTVDPVKKALKDARITASDISDVLLVGGMSRMPKVADTVKKLFGKDASKAVNPDEAVALGAAIQAAVLSGEVTDVLLLDVTPLSLGIETLGGVFTKLIPRNSTIPNKKSQIFSTAASGQTSVEVKVFQGERELVKDNKLIGNFTLAGIPPAPKGTPQIEVTFDIDANGIINVSAKDLASHKDSSITVAGASGLSDTEIDRMVNEAERYKNQDRARRNAIETANKADQLANDTENSIKEFEGKLDKTDSQRLKDQISSLRELVSRSQAGDEVNDDDVGTKIDNLRTSSMKLFEQLYKNSDNPETKNGRENK";
////        String proteinSequence = "WFPCWGIYHQCLNEVSTAQQILKDVQEPVDLDNFRRIDNGTLKDTIRKLVLMARANRIANKHENEVRQVEEETIAGNSLLENANMVPLQIGTEEEIKKTPLDFGWNILPDFAFAELIAMLSGKNDRLVKMVNECTIRFSGEIGSVEMAYTLMRTLRFPVKEPFKERLIAAEFCDGFDIHIVKGTIRDLMLNSPHRDGLGLIYGTMSMVALSRTYTTRRELWTESSRSKLWLVKYLDQGETNNLAYTFVEVKQLLTLNDYDPAMQLMVWHEINLPIKKAERHERILVHFTDSNPVWGLLGSKPSLPIAPYQQIDLHRRFCEADNQLLTNVLGFLQMVLSDQRIDEHGKLVYKYDKGDSGKICFKRPRQKSSIVSFVPEFKSIKVIPKGGSARTGPVALELDHASLLKPSVHQLELTQLQPLQKGIKRFVNYYIDWAQNLNSVDKSKKYNMLWEYADNLDRGFSNQFSIERLTEPGRKLMEYLPELAAFMKETNHEGFFQRSADDLGEYWQEHWLVAMRILEHSVLEAQDVLVPSHIRMKEIISLAAKQRSLSESKIAVMLPYVLAQPHAKGLDSLLSLLSRSVIQNPQHIRSILQPLVELWTGIQILNFGEHMAQTAEPIGGFTFWLTLLRLADQLSSSESLSISHFFGKIAPIVHRHILNSSYHVEKADFTNVGIMGNDFENIDTVSSADSGEQKKKSVSTLMSIVEFNALAWNHWAKYWTNDFHTALLYSGLISDPNSLRWKPQLCVRWEGQKLFCRALLKTYDEVHRPVRKSQQPVSQAIMNNPDLGLDHAMRSTFNILQKLAEDQLGTAWLYKLQAYVVPPSAKATNPHDPDDTEELLTNLVKKALAMRGSKRCLNAFKIRVQADEKPKIVLSRVRLIRQWVDINKQCGLLRTNWTERMTLRKDSNQPLKKYKIIEELEAIIQARVVVNYARNYSENVLASLETVLLDRANFIHVEAKKFNNRHLCLIADYFEKDPSQSKMVSTYQAIEDWQELGWAAGAALPAMAKKVEPKATGWKESALKSLEEWEGLAYLSRLKGMMVEVSDEGAAEKENYAALADEWRQLKEYWTEKLQLENHQQAHKLIGIASDTQHLQNNISILAEITSNKPEELFEVEKYHLAKAFAHCKQAYKGLTHIPIPLPKDDHEMFEVLNLLMQYIEPPNESSSLAKCLAQILDEQYSTQLEVWCSSFSANFLERALPYYVSVLSSCSRLCASPSEKLLQISLRRIWEQWDEKTKQQSCYWANKLINQNVPLKTVQMEDEYNKREPVENEKDFIINTPLCENNLLKNVLQDYVSHQIRNRLLAKNIVPVFVVFDTGLQLLLLSLTNMTAKTLERDGNNLIRVLAQVIRSSMESLNINKALRGLTIISIKKLSGASYETMRVVIPMILHSYDELNPGFTVLSKLIRIPVIRKNSQDNELIDLFFTLTEPVFRKFEGELAKSISEIVSIITIQLKIIPFFERIVGYIKEVHPRIHQKVISILSGLQQFYFDLQSPPCSRMVLIIGPIIQDLFSVCRLGLNQFIHMIAQIAATHHISLSPDNLIKMLNHIVVTPYYEDNSPSVGQMLLAIDISPANQEVSSKSNSTVEIERHKYPDLAGLIGILRVTGRRIHPNNETKLINILIGLLEPYDLLPGVVYGSSAALQGLTTLAADRKFSNSQDQFTNIILPMLEKLYRTMEKGGVVSLEGLVKLATSAVASSADQCKPLIVDLIPDIYPKAVEDSSNILTCLLTASEEKKKPMNSFKLQTLLELLTKRLSPVVYAPNVSSLRGIIKIAELQIGFIEDNLAMFLLRLNDPQALQPDFNSGLHQLIELRIEAVPDTIAIMLLKSLVESVSHLAHVSTQKCIDDKIFLDCSTLAALKRVSSDEHEIYSITILRVFETLSYQHHILQLMKFCQILIQADTIDDNSEGTKKMFSQNRSKRAKEISFQNNFDYQNSQIFKEGSLSISLLNLIRSNVTSELSPIKENLIMLTEQMHDSMPCNLMLNLLDKNLHKAFAPGLACALKGICYFLDKEFQKRVKFKTRLGERINDLILTMYPSISSGVEFAIDGISVLIFPKDSNNAANMDINKLYRLYHVMIRDLYKKTFIAPDFAALLPLIAYVERRIVDFKYEKYKMTSKYIDDYKDRLYPAKLSLLERFVLLTAHVSDNTNLSLGHTCGQFLRQFWQKGLAPDRDQIITLCKGLAVAADLRIILKADRLPVWINDLISNVYPYLLYPSNDALAKIILLAAHRRYELKSSSSNNDATLTLWDICTRVEFEVFDSTLTGGPVTLRGLTNAALRMVEIDSSPILVRLYNALRSTQNPLEETSLYFSILTDVALIGGIKESSTFGHILEFIKNNLSNSFRQFQEASVERALSTLTTSLENAGSAREQPVDSKLKDFILNLTSFTTDLESDVFSIKGIHGASGTIVRGSDNPGNHNSDTNSTTSMEDDHSHSKHTLKKRTRHKGEARQRLSLLNPPTTYKNIYKNM";
//        int minEnzymaticTermini = 1;
//        String peptide = "IIENAEGSRTTPS";
////        String peptide = "LLTNVLGFLQMVLSDQRIDEHG";
//        MsEnzyme enzyme = new MsEnzyme() {
//            @Override
//            public int getId() {
//                return 0;
//            }
//            @Override
//            public String getCut() {
//                return "KR";
//            }
//            @Override
//            public String getDescription() {
//                return null;
//            }
//            @Override
//            public String getName() {
//                return "Trypsin_K";
//            }
//            @Override
//            public String getNocut() {
//                return "P";
//            }
//
//            @Override
//            public Sense getSense() {
//                return Sense.CTERM;
//            }};
//        EnzymeRule rule = new EnzymeRule(enzyme);
//        List<EnzymeRule> rules = new ArrayList<EnzymeRule>(1);
//        rules.add(rule);
//        
//        NrDbProtein dbProt = new NrDbProtein();
//        dbProt.setAccessionString("YEL030W");
//        dbProt.setDatabaseId(178);
//        dbProt.setProteinId(529942);
//        
//        PeptideProteinMatchingService service = new PeptideProteinMatchingService(178);
//        
//        PeptideProteinMatch match = service.getPeptideProteinMatch(dbProt, peptide, proteinSequence);
//        
//        assertNotNull(match);
//        assertEquals(1, match.getNumEnzymaticTermini());
//	}

}
