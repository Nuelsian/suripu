import com.google.common.base.Optional;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by benjo on 2/21/15.
 */
public class HmmTest {

    final double [][] A = {{0.9725484971360321, 0.009002873283940086, 0.0006013017515543635, 0.008400835790670077, 0.0, 0.0, 0.0, 0.009446492037859492},
                    {0.0, 0.902848112734089, 0.042541502131080676, 0.05461038513486347, 0.0, 0.0, 0.0, 0.0},
                    {0.0, 0.0030389389157415713, 0.8562189946486717, 0.1407420664355919, 0.0, 0.0, 0.0, 0.0},
                    {0.0, 0.0, 0.0, 0.9217795011862617, 0.056696930484166436, 0.012913452470455063, 0.008610115859104274, 0.0},
                    {0.0, 0.0, 0.0, 0.26241536584756414, 0.7375846341524513, 0.0, 0.0, 0.0},
                    {0.006706338236820103, 0.0, 0.0, 0.0, 0.0, 0.9780951506204674, 0.010105252677320568, 0.0050932584653425724},
                    {0.008856536527285842, 0.0, 0.0, 0.0, 0.0, 0.0, 0.9290170368381464, 0.062126426634585945},
                    {0.005350596740707045, 0.008303072033020591,0.0002574216620331236, 0.002068835583603125, 0.0, 0.0, 0.0, 0.9840200739806679}};

    final double [] pi = {1.5356594161966379e-33, 0.9999999999999483, 2.181658599210267e-26, 1.8038299006856717e-53, 1.2535075955723791e-69, 3.307222075125561e-30, 5.177034031709679e-14, 4.249801363195405e-21};


    final double [][] model = {
        {2.1226136275434646,0.01},
        {8.949864212072578,1.3725002589799848},
        {2.366872233436011,2.3755910741352704},
        {1.8802515919264213,0.27336547789612975},
        {1.8711165971541512,1.2576592366004111},
        {5.84323045913544,0.040171094093295015},
        {6.858000619686331,0.9552711227217422},
        {8.904149154579438,0.027138466156016776}};

    final double [][] model2 = {
            {2.1226136275434646,0.01},
            {8.949864212072578,1.3725002589799848},
            {2.366872233436011,2.3755910741352704},
            {1.8802515919264213,1.0},
            {1.8711165971541512,1.2576592366004111},
            {5.84323045913544,0.040171094093295015},
            {6.858000619686331,0.9552711227217422},
            {8.904149154579438,0.027138466156016776}};


    double [][] mydata = {{ 6,7,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
8,8,8,8,4,2,2,2,2,2,2,2,2,2,4,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,3,3,8,10,9,4,4,4,4,
4,4,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,6,6,6,6,6,
6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,7,6,6,
6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,7,7,7,7,7,7,
7,9,9,8,8,8,8,8,8,8,8,8,7,7,7,7,7,7,7,7,7,7,7,7,
7,7,7,7,7,7,7,7,7,7,7,6,6,6,6,6,6,6,5,5,5,10,10,10,
10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,9,7,7,7,7,7,7,7,7,
7,7,7,7,7,7,7,7,7,7,8,8,8,8,8,8,7,7,7,7,7,7,7,7,
7,7,7,7,7,7,7,6,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,3,4,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,3,3,8,9,8,5,8,6,6,6,6,6,
6,6,7,7,7,7,7,7,7,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
8,8,8,8,9,9,9,9,9,9,9,9,9,9,10,10,10,10,10,10,10,10,10,10,
10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,
10,10,10,10,10,10,10,10,10,11,11,11,11,11,11,11,11,11,11,11,11,10,11,11,
11,11,11,10,10,10,10,10,10,10,10,10,10,10,10,10,9,9,9,9,8,8,7,7,
6,6,5,5,4,3,2,2,2,2,2,2,2,2,2,2,2,2,2,2,5,5,4,3,
3,3,3,3,7,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,7,7,7,
7,2,3,2,2,5,2,2,2,5,6,5,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,3,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,3,7,10,10,10,10,6,6,6,6,7,
6,7,6,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,8,8,8,
8,8,8,8,8,8,8,8,8,8,8,8,8,11,11,11,11,10,10,11,11,11,11,11,
11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,
11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,
11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,10,7,7,6,
6,6,5,5,5,4,4,4,4,4,4,4,4,4,8,10,10,10,10,10,10,10,10,10,
10,10,10,9,7,7,8,8,8,7,7,7,7,7,7,7,8,7,7,7,7,7,7,7,
7,7,7,7,7,7,7,7,7,6,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
7,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
2,2,2,2,2,2,2,2,2,2,2,2,2,6,7,7,8,9,9,5,8,9,9,9,
9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,
9,9,9,9,9,9,9,9,10,10,10,10,10,10,10,10,10,10,9,9,9,9,9,9,
9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,10,10,10,10,10,
10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
{4,5,0,1,1,1,1,2,2,3,3,2,1,4,4,5,5,5,4,4,3,3,5,1,
2,0,1,0,5,1,1,0,0,1,0,0,0,0,3,5,3,0,1,0,1,1,2,1,
0,4,1,0,0,0,0,0,0,0,0,2,0,0,0,2,1,0,0,0,0,1,1,0,
0,2,1,3,2,0,0,2,0,1,1,1,0,0,3,1,1,2,1,0,0,1,1,0,
1,0,1,1,2,2,1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1,0,1,
0,1,0,0,0,0,0,0,0,1,0,0,0,1,3,0,0,0,1,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,
0,0,0,0,0,0,0,0,0,1,1,3,0,0,1,0,0,0,0,1,1,3,2,2,
2,2,4,4,2,4,4,3,3,3,2,0,1,0,2,0,5,1,4,3,3,3,4,3,
3,2,2,4,2,2,1,4,5,5,5,5,5,4,4,4,2,0,1,1,0,2,1,0,
0,0,0,4,2,0,0,1,2,1,1,1,1,0,2,0,0,1,2,0,0,2,0,1,
0,0,0,1,2,0,0,1,1,1,0,0,0,0,0,2,1,1,1,0,0,0,0,0,
1,0,0,0,3,1,2,0,0,1,0,0,0,0,2,0,1,0,1,0,0,1,0,0,
2,3,0,1,0,2,0,0,2,2,1,1,4,0,1,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,2,0,1,3,
4,2,4,5,4,4,4,3,5,2,1,5,3,5,5,2,0,1,0,0,1,1,0,1,
0,1,0,0,0,2,0,0,0,0,1,0,3,1,0,0,0,2,0,0,1,1,0,0,
1,0,1,2,1,0,0,1,0,2,1,1,1,2,0,0,0,0,1,0,0,0,1,0,
2,0,0,1,1,0,1,0,3,1,0,2,0,1,1,0,0,0,0,1,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,1,4,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,3,4,0,2,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,
0,0,1,0,3,1,0,4,1,2,0,2,0,2,3,2,4,3,5,5,3,0,0,1,
0,2,0,4,2,0,0,4,1,2,0,0,0,0,0,1,0,1,0,0,0,0,0,0,
3,5,1,0,0,4,0,1,0,3,0,0,0,1,4,2,0,0,0,2,2,1,0,0,
1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,2,1,1,0,1,1,0,1,1,
0,0,1,0,2,0,0,1,1,1,1,1,1,0,2,0,3,2,1,0,0,0,0,0,
0,3,0,0,0,1,2,0,1,2,0,4,4,4,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}};

    int [] path = {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 0};


    public static HiddenMarkovModel createPoissonOnlyModel(double [][] A, double [] initialStateProbs, double [][] poissonMeans) {
        final int numStates = A.length;
        final int numObsModelsPerState = poissonMeans[0].length;

        HmmPdfInterface[] models = new HmmPdfInterface[numStates];

        for (int j = 0; j < numStates; j++) {
            PdfComposite c = new PdfComposite();
            for (int i = 0; i < numObsModelsPerState; i++) {
                c.addPdf(new PoissonPdf(poissonMeans[j][i],i));
            }

            models[j] = c;

        }

        return new HiddenMarkovModel(numStates,A,initialStateProbs,models,1);
    }

    @Test
    public void testDecode() {
        HiddenMarkovModel hmm = createPoissonOnlyModel(A, pi, model);

        final Integer [] possibleEndStates = {0};
        HmmDecodedResult result =  hmm.decode(this.mydata, possibleEndStates);


        for (int t = 0; t < result.bestPath.size(); t++) {
            if (result.bestPath.get(t) != this.path[t]) {
                int foo = 3;
                foo++;
            }

            TestCase.assertTrue(result.bestPath.get(t) == this.path[t]);
        }

    }

}
