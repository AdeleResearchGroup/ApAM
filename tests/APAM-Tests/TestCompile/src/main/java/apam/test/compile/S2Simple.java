/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package apam.test.compile;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s3.S3_1;
import fr.imag.adele.apam.test.s4.S4;

public class S2Simple implements S2, ApamComponent {

    S3_1 fieldS3;
    S4   s4;

    String name;
    String state ;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void apamInit(Instance inst) {
        name = inst.getName();
        System.out.println("S2Simple Started : " + inst.getName());
    }

    @Override
    public void callS2(String s) {
        System.out.println("S2 simple called :" + s);
        fieldS3.callS3_1toS5("from S2Simple to S3_1toS5 ");
        fieldS3.callS3_1("from S2Simple to S3_1 ");
        s4.callS4_final("from S2Simple ");
    }

    @Override
    public void callBackS2(String s) {
        // TODO Auto-generated method stub
        s4.callBackS4("from s2-inv; call back");
    }

    @Override
    public void apamRemove() {
        // TODO Auto-generated method stub

    }
}
