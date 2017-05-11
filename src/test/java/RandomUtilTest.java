import ib053.core.RandomUtil;

/**
 *
 */
public class RandomUtilTest {

    public static void main(String[] args){
        int sum = 0;
        final int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            sum += RandomUtil.chooseFirst(2,1) ? 1 : 0;
        }
        System.out.println("chooseFirst: Got: "+((float)sum * 100f/(float)iterations)+"%, expected 66%");

        sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += RandomUtil.check(0.66666666f) ? 1 : 0;
        }
        System.out.println("check: Got: "+((float)sum * 100f/(float)iterations)+"%, expected 66%");
    }

}
