package Services;

import org.junit.Assert;
import org.junit.Test;

public class NlpMlServiceTest {

    @Test
    public void testCategorizeSamples() throws Exception {
        NlpMlService nlp = new NlpMlService();

        String eBest = nlp.classifyPillar("énergie émissions CO2 efficacité procédés");
        String sBest = nlp.classifyPillar("sécurité employés santé formation");
        String gBest = nlp.classifyPillar("gouvernance conformité audit transparence");

        Assert.assertEquals("E", eBest);
        Assert.assertEquals("S", sBest);
        Assert.assertEquals("G", gBest);
    }
}
