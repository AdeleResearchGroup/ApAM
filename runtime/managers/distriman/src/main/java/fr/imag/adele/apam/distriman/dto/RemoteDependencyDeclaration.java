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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.InstanceReference;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.SpecificationReference;

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
public class RemoteDependencyDeclaration extends RelationDeclaration {
    private static final String JSON_COMP_REF_NAME = "component_name";
    private static final String JSON_RESOLVABLE_REF = "rref";
    private static final String JSON_RESOLVABLE_REF_TYPE = "type";
    private static final String JSON_RESOLVABLE_REF_NAME = "name";
    private static final String JSON_ID = "id";
    private static final String JSON_IS_MULTIPLE = "is_multiple";

    private static final String JSON_INSTANCE_CONSTRAINT = "instance_constraint";
    private static final String JSON_IMPLEMENTATION_CONSTRAINT = "implementation_constraint";
    private static final String JSON_INSTANCE_PREF = "instance_preference";

    private static final String JSON_COMP_CONSTRAINT = "comp_cons";
    private static final String JSON_COMP_PREF = "comp_pref";
    public static final String JSON_PROVIDER_URL = "provider_url";
    
    private String providerURL;
    
    public RemoteDependencyDeclaration(ComponentReference<?> component, String id, boolean isMultiple, ResolvableReference resource,String provider) {
		super(component, id, resource, isMultiple);
        this.providerURL=provider;
    }

    /**
     * Wrapper around a DependencyDeclaration.
     * @param dep
     */
    public RemoteDependencyDeclaration(Relation dep,String provider) {
    	
     	super(new ComponentReference<ComponentDeclaration>(dep.getLinkSource().getName()), dep.getIdentifier(), dep.getTarget(), dep.isMultiple());
    	
        this.getImplementationConstraints().addAll(dep.getImplementationConstraints());
        this.getInstanceConstraints().addAll(dep.getInstanceConstraints());
        this.getImplementationPreferences().addAll(dep.getImplementationPreferences());
        this.getInstancePreferences().addAll(dep.getInstancePreferences());

//        this.getImplementationConstraints().setMissingException(dep.getMissingException());
//        this.getImplementationConstraints().setMissingPolicy(dep.getMissingPolicy());

        
        this.providerURL=provider;
    }

    public ObjectNode toJson() {
    	
    	ObjectMapper om=new ObjectMapper();
    	
    	ObjectNode root = om.createObjectNode();
        
        root.put(JSON_ID,getIdentifier());
        root.put(JSON_IS_MULTIPLE,isMultiple());
        root.put(JSON_COMP_REF_NAME,getComponent().getName());
        root.put(JSON_PROVIDER_URL,providerURL);
        
        ObjectNode json_rr=om.createObjectNode();
        
        json_rr.put(JSON_RESOLVABLE_REF_NAME,getTarget().getName());


        //Set RRTYPE
        if (getTarget() instanceof InterfaceReference){
            json_rr.put(JSON_RESOLVABLE_REF_TYPE,RRTYPE.itf.toString());
        } else if (getTarget() instanceof MessageReference){
            json_rr.put(JSON_RESOLVABLE_REF_TYPE,RRTYPE.message.toString());
        } else if (getTarget() instanceof InstanceReference){
            json_rr.put(JSON_RESOLVABLE_REF_TYPE,RRTYPE.instance.toString());
        }

        //Set the ResolvableReference
        root.put(JSON_RESOLVABLE_REF,json_rr);
        
        ArrayNode instanceconstraints = om.createArrayNode();
        ArrayNode implementationconstraints = om.createArrayNode();

        for(String filter:this.getInstanceConstraints()){
        	instanceconstraints.add(filter);
        }
        
        for(String filter:this.getImplementationConstraints()){
        	implementationconstraints.add(filter);
        }
        
        root.put(JSON_INSTANCE_CONSTRAINT, instanceconstraints);
        root.put(JSON_IMPLEMENTATION_CONSTRAINT, implementationconstraints);
        //json.put(JSON_INSTANCE_CONSTRAINT, new JSONArray(getInstanceConstraints()));
        //json.put(JSON_INSTANCE_PREF, new JSONArray(getInstancePreferences()));
        //json.put(JSON_COMP_CONSTRAINT, new JSONArray(getImplementationConstraints()));
        //json.put(JSON_COMP_PREF, new JSONArray(getImplementationPreferences()));

        return root;
    }



    /**
     * Create a RemoteDependency from a JSONObject.
     * @param json The JSONObject version of the RemoteDependency.
     * @return RemoteDependency corresponding to <code>json</code>
     * @throws IllegalArgumentException
     */
    public static RemoteDependencyDeclaration fromJson(JsonNode json) throws IllegalArgumentException{
    	JsonNode rr_json = json.get(JSON_RESOLVABLE_REF);

        //Get the RemoteDependency id
        String id = json.get(JSON_ID).asText();

        //Get the RemoteDependency is multiple boolean
        Boolean multiple = json.get(JSON_IS_MULTIPLE).getBooleanValue();

        //Create the ComponentReference from its  name
        ComponentReference<?> compref = new ComponentReference(json.get(JSON_COMP_REF_NAME).asText());

        //Get the ResolvableReference type       
        RRTYPE rr_type = RRTYPE.valueOf(rr_json.get(JSON_RESOLVABLE_REF_TYPE).asText());

        //Get the ResolvableReference name
        String rr_name = rr_json.get(JSON_RESOLVABLE_REF_NAME).asText();

        String providerURL=json.get(JSON_PROVIDER_URL).asText();

        ResolvableReference  rr = null;
        // Create the ResolvableReference according to its type.
        switch (rr_type){
            case instance:
                rr=new InstanceReference(rr_name);
            break;
            case itf: 
                rr=new InterfaceReference(rr_name);
            break;
            case message:
                rr = new MessageReference(rr_name);
            break;
            case specification:
                rr = new SpecificationReference(rr_name);
                
            break;
        }

        //Get constraints and prefs

//        JSONArray instconst = json.getJSONArray(JSON_INSTANCE_CONSTRAINT);
//        JSONArray instpref = json.getJSONArray(JSON_INSTANCE_PREF);
//        JSONArray compconst = json.getJSONArray(JSON_COMP_CONSTRAINT);
//        JSONArray comppref = json.getJSONArray(JSON_COMP_PREF);

        RemoteDependencyDeclaration rdep = new  RemoteDependencyDeclaration(compref,id,multiple,rr,providerURL);

        ObjectMapper om=new ObjectMapper();
        
        List<String> instanceconstraints=om.convertValue(json.get(JSON_INSTANCE_CONSTRAINT), new TypeReference<ArrayList<String>>() {});
        
        rdep.getInstanceConstraints().addAll(instanceconstraints);
        
        //rdep.getInstanceConstraints().addAll(fromArray(instconst));
        //rdep.getInstancePreferences().addAll(fromArray(instpref));
        //rdep.getImplementationConstraints().addAll(fromArray(compconst));
        //rdep.getImplementationPreferences().addAll(fromArray(comppref));

       return rdep;
    }

    /**
     * ResolvableReference type.
     */
    private enum RRTYPE {
        instance,
        message,
        itf,
        specification
    }

	public String getProviderURL() {
		return providerURL;
	}
    
}
