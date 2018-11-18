import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NPC {
    private final String name;
    private int room;
    private LinkedList<String> quests;
    private ArrayList<DialogueOption> dialogueList;

    //These fields are for the team 6 main story implementation of NPC talking
    private ArrayList<String> introDialogues;
    private ArrayList<String> contDialogues;
    private ArrayList<String> doneDialogues;
    private ArrayList<String> conditions;
    private ArrayList<String> status;
    private LinkedList<Integer> validQuests;
    private boolean validDialogue;


    public NPC(String name, int room) {
        this.name = name;
        this.room = room;
        validDialogue = setDialogues("./NPCDialogues/" + name + "/Dialogue.xml");
    }

    public NPC(String name, int room, LinkedList<String> quests, ArrayList<DialogueOption> dialogueList) {
        this.name = name;
        this.room = room;
        this.quests = quests;
        this.dialogueList = dialogueList;
        readXMLDialogue();
	//Sets flag to differentiate between team 6 Main Story compatable NPCs and Tutorial Quest NPCs
	validDialogue = false;
    }

    public String getName() {
        return name;
    }

    public List<String> getQuests(){
        return quests;
    }

    public ArrayList<DialogueOption> getDialogueList() {
        return dialogueList;
    }

    @Override
    public String toString(){
        return "NPC " + getName();
    }

    public boolean checkValidDialogue(){return validDialogue;}

    private boolean setDialogues(String fileName){
	try{
	    introDialogues = new ArrayList<>();
	    contDialogues = new ArrayList<>();
       	    doneDialogues = new ArrayList<>();
	    conditions = new ArrayList<>();
	    status = new ArrayList<>();

	    validQuests = new LinkedList<>();

            File dialFile = new File(fileName);
	    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.parse(dialFile);
       
            document.getDocumentElement().normalize();
            NodeList xmlDial = document.getElementsByTagName("dialogue");

	    String dialogue;
	    Element dialogueElement;
	    int id;

	    introDialogues.add(0,null);
            contDialogues.add(0,null);
            doneDialogues.add(0,null);
	    conditions.add(0,null);
	    status.add(0,null);
	    
	    
	    for(int i = 0; i < xmlDial.getLength(); i ++){
                dialogueElement = (Element) xmlDial.item(i);
		id = Integer.parseInt(dialogueElement.getAttribute("id"));
                for(int j = introDialogues.size(); j <= id; j ++){
                    introDialogues.add(j,null);
                    contDialogues.add(j,null);
                    doneDialogues.add(j,null);
		    conditions.add(j,null);
		    status.add(j,null);
                }

		if(id != -1){
		    validQuests.add(id);

                    dialogue = dialogueElement.getElementsByTagName("intro").item(0).getTextContent();
	            introDialogues.set(id,dialogue);

	            dialogue = dialogueElement.getElementsByTagName("cont").item(0).getTextContent();
                    contDialogues.set(id,dialogue);

	            dialogue = dialogueElement.getElementsByTagName("done").item(0).getTextContent();
                    doneDialogues.set(id,dialogue);

                    dialogue = dialogueElement.getElementsByTagName("condition").item(0).getTextContent();
                    conditions.set(id,dialogue);

		    dialogue = dialogueElement.getElementsByTagName("status").item(0).getTextContent();
                    status.set(id,dialogue);
		}else{
		    dialogue = dialogueElement.getElementsByTagName("intro").item(0).getTextContent();
                    introDialogues.set(0,dialogue);
		}
	    }
        } catch (IOException ex1){
            //System.out.println("[WORLD CREATION] Invalid Or No Dialogue For NPC: " + name + " In Room ID: " + room);
	    return false;
	} catch (ParserConfigurationException | SAXException ex2) {
            Logger.getLogger(NPC.class.getName()).log(Level.SEVERE, null, ex2);
	} 
	return true;
    }

    public String talk(Player player){
	if(!validDialogue)
            return name + " looks at you, and says nothing";
        int progress = player.getProgress();
//	System.out.println("Getting quest " + progress);
	int dialId = (progress / 2)+1;
	String dial = "";//Short for dialogue
	if(validQuests.indexOf(dialId) == -1)
	    return introDialogues.get(0);
	switch(progress % 2)
	{
            case 0:
	        dial = introDialogues.get(dialId);
		if(dial != null){
                    player.advanceQuest();
		    if(conditions.get(dialId).equals("RPS"))
			player.setRpsVictoryCount(0);
		}
		break;
            case 1:
		if(checkCondition(player, conditions.get(dialId), status.get(dialId))){
                    dial = doneDialogues.get(dialId);
		    player.advanceQuest();
		}else{
		    dial = contDialogues.get(dialId);
		}
	}
        return dial;
    }
 
    /* Checks the player's current state and returns whether they meet the condition and status checks
     * Uses team 6 Main Story NPC implementation.
     *
     * @param player Player to check conditions for
     * @param condition Condition to check
     * @param status Expected status of the condition
     *
     * @return True if the condition has the desired status
     */
    public boolean checkCondition(Player player, String condition, String status){
	//System.out.println("Expected " + condition + "=" + status);
        int temp, temp2;//Temporary integer used for checking status's
	switch (condition){
            case "TITLE":
		//System.out.println("Found " + player.getTitle());
	        if(player.getTitle().equals(status))
		    return true;
		else
	            return false;
	    case "INVENTORY":
		if(status.substring(0,1).equals("x"))
                    temp = Integer.parseInt(status.substring(1,2));
		else
		    temp = 1;
		temp2 = 0;
		for (Item i : player.getCurrentInventory())
		{
                if(i.getName().equals(status.substring(3,status.length())))
		temp2 ++;
		}
		if(temp2 >= temp)
		    return true;
		return false;
	    case "RPS":
	       	if(player.getRpsVictoryCount() >= Integer.parseInt(status))
		    return true;
		return false;
	    case "SUCCESS":
		return true;
	}
	return false;
    }

    public boolean changeDialogueList(String dialogueTag, int changeTagId)
    {
        for (int i = 0; i < dialogueList.size(); i++) {
            if (dialogueList.get(i).usingTag() && dialogueList.get(i).getTag().equals(dialogueTag))
            {
                dialogueList.get(i).changeDialogueId(changeTagId);
                return true;
            }
        }
        return false;
    }

    public boolean incrementDialogueList(String dialogueTag)
    {
        for (int i = 0; i < dialogueList.size(); i++) {
            if (dialogueList.get(i).usingTag() && dialogueList.get(i).getTag().equals(dialogueTag))
            {
                dialogueList.get(i).changeDialogueId(dialogueList.get(i).getDialogueId() + 1);
                return true;
            }
        }
        return false;
    }

    public int getDialogueId(String dialogueTag)
    {
        for (int i = 0; i < dialogueList.size(); i++) {
            if (dialogueList.get(i).usingTag() && dialogueList.get(i).getTag().equals(dialogueTag))
            {
                return dialogueList.get(i).getDialogueId();
            }
        }
        
        return -1;
    }

    public void addToDialogueList(String dialogueTag, String prompt)
    {
        dialogueList.add(new DialogueOption(prompt, dialogueTag, true));
    }

    private void readXMLDialogue()
    {
        try {
            File commandFile = new File("./NPC_Dialogue.xml");
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.parse(commandFile);

            document.getDocumentElement().normalize();
            NodeList xmlNPC = document.getElementsByTagName("npc");
            NodeList xmlDialogueType;
            NodeList xmlDialogueId;

            String prompt;
            String dialogueTag;
            Element xmlElement;
            Element xmlDialogueElement;
            Element xmlDialogueIdElement;

            for (int i = 0; i < xmlNPC.getLength(); i++) {
                xmlElement = (Element) xmlNPC.item(i);

                if (name.equals(xmlElement.getAttribute("name")))
                {
                    xmlDialogueType = xmlElement.getElementsByTagName("dialogue_type");
                    for (int j = 0; j < xmlDialogueType.getLength(); j++) 
                    {
                        xmlDialogueElement = (Element) xmlDialogueType.item(j);
                        prompt = xmlDialogueElement.getElementsByTagName("dialogue_prompt").item(0).getTextContent();
                        dialogueTag = xmlDialogueElement.getAttribute("type");

                        dialogueList.add(new DialogueOption(prompt,dialogueTag,true));
                    }

                    break;
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(NPC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
