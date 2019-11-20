# Projects
Keeps a list of the available ZMLP projects in the Wallet database. The main use of this 
app is to associate Users with projects and manage the ZMLP API keys required for those 
interactions. When a User is added to a Project a ZMLP API key is stored along with that 
relationship that allows Wallet to communicate with ZMLP for the given project on behalf 
of the user. 

This app also contains a base ViewSet that can used by other apps that need to communicate
with ZMLP in a project context. It automatically handled generating a REST client for the 
view that is authenticated with the correct API key for the user. 
