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
package fr.imag.adele.apam.distriman.dto;

import fr.imag.adele.apam.declarations.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The RemoteDependency is a DependencyDeclaration that aims to be resolved by a RemoteMachine.
 * It contains two convenient serialisation methods: <code>fromJson</code> that allows for the creation of a
 * RemoteDependency object from a JSONObject and <code>toJson</code> that return a JSONObject corresponding to this
 * RemoteDependency.
 *
 * User: barjo
 * Date: 14/12/12
 * Time: 14:42
 */
public class RemoteDependency extends DependencyDeclaration {
    private static final String JSON_COMP_REF_NAME = "component_name";
    private static final String JSON_RESOLVABLE_REF = "rref";
    private static final String JSON_RESOLVABLE_REF_TYPE = "type";
    private static final String JSON_RESOLVABLE_REF_NAME = "name";
    private static final String JSON_ID = "id";
    private static final String JSON_IS_MULTIPLE = "is_multiple";

    private static final String JSON_INSTANCE_CONSTRAINT = "instance_cons";
    private static final String JSON_INSTANCE_PREF = "instance_pref";

    private static final String JSON_COMP_CONSTRAINT = "comp_cons";
    private static final String JSON_COMP_PREF = "comp_pref";

    public RemoteDependency(ComponentReference<?> component, String id, boolean isMultiple, ResolvableReference resource) {
        super(component, id, isMultiple, resource);
    }

    /**
     * Wrapper around a DependencyDeclaration.
     * @param dep
     */
    public RemoteDependency(DependencyDeclaration dep) {
        super(dep.getComponent(), dep.getIdentifier(), dep.isMultiple(), dep.getTarget());
    }

    public JSONObject toJson() throws JSONException{
        JSONObject json = new JSONObject();
        JSONObject json_rr = new JSONObject();

        json.put(JSON_ID,getIdentifier());
        json.put(JSON_IS_MULTIPLE,isMultiple());
        json.put(JSON_COMP_REF_NAME,getComponent().getName());
        json_rr.put(JSON_RESOLVABLE_REF_NAME,getTarget().getName());


        //Set RRTYPE
        if (getTarget() instanceof InterfaceReference){
            json_rr.put(JSON_RESOLVABLE_REF_TYPE,RRTYPE.itf);
        } else if (getTarget() instanceof MessageReference){
            json_rr.put(JSON_RESOLVABLE_REF_TYPE,RRTYPE.message);
        } else if (getTarget() instanceof InstanceReference){
            json_rr.put(JSON_RESOLVABLE_REF_TYPE,RRTYPE.instance);
        }

        //Set the ResolvableReference
        //json.put(JSON_RESOLVABLE_REF,json_rr);

        //json.put(JSON_INSTANCE_CONSTRAINT, new JSONArray(getInstanceConstraints()));
        //json.put(JSON_INSTANCE_PREF, new JSONArray(getInstancePreferences()));
        //json.put(JSON_COMP_CONSTRAINT, new JSONArray(getImplementationConstraints()));
        //json.put(JSON_COMP_PREF, new JSONArray(getImplementationPreferences()));

        return json;
    }



    /**
     * Create a RemoteDependency from a JSONObject.
     * @param json The JSONObject version of the RemoteDependency.
     * @return RemoteDependency corresponding to <code>json</code>
     * @throws JSONException
     * @throws IllegalArgumentException
     */
    public static RemoteDependency fromJson(JSONObject json) throws JSONException,IllegalArgumentException{
        JSONObject rr_json = json.getJSONObject(JSON_RESOLVABLE_REF);

        //Get the RemoteDependency id
        String id = json.getString(JSON_ID);

        //Get the RemoteDependency is multiple boolean
        Boolean multiple = json.getBoolean(JSON_IS_MULTIPLE);

        //Create the ComponentReference from its  name
        ComponentReference<?> compref = new ComponentReference(json.getString(JSON_COMP_REF_NAME));

        //Get the ResolvableReference type
        RRTYPE rr_type = RRTYPE.valueOf(rr_json.getString(JSON_RESOLVABLE_REF_TYPE));

        //Get the ResolvableReference name
        String rr_name = rr_json.getString(JSON_RESOLVABLE_REF_NAME);


        ResolvableReference  rr = null;
        // Create the ResolvableReference according to its type.
        switch (rr_type){
            case instance:
                rr=new InstanceReference(rr_name);
            break;
            case itf: //interface
                rr=new InterfaceReference(rr_name);
            break;
            case message:
                rr = new MessageReference(rr_name);
            break;
        }

        //Get constraints and prefs

//        JSONArray instconst = json.getJSONArray(JSON_INSTANCE_CONSTRAINT);
//        JSONArray instpref = json.getJSONArray(JSON_INSTANCE_PREF);
//        JSONArray compconst = json.getJSONArray(JSON_COMP_CONSTRAINT);
//        JSONArray comppref = json.getJSONArray(JSON_COMP_PREF);


        RemoteDependency rdep = new  RemoteDependency(compref,id,multiple,rr);

        //rdep.getInstanceConstraints().addAll(fromArray(instconst));
        //rdep.getInstancePreferences().addAll(fromArray(instpref));
        //rdep.getImplementationConstraints().addAll(fromArray(compconst));
        //rdep.getImplementationPreferences().addAll(fromArray(comppref));

       return rdep;
    }


    private static Collection<String> fromArray(JSONArray array) throws JSONException{
        Collection<String> collec = new ArrayList<String>(array.length());
        for(int i =0;i<array.length();i++){
            collec.add(array.getString(i));
        }
        return collec;
    }

    /**
     * ResolvableReference type.
     */
    private enum RRTYPE {
        instance,
        message,
        itf//interface
        //TODO support Spec et implem ?
    }
}
