/** COEN 275 OOAD (Winter 2016)
 *  Group Project
 *  Team 8
 */
package market;
import users.Portfolio;

import java.io.IOException;
import java.util.*;

import users.Authentication;
import users.User;
import utility.Logger;

public class Market extends Thread {


    private static Market marketInstance = null;
    private List<Stock> globalStocks = new ArrayList<Stock>();
    private static final List<BuySell> buyRequest = Collections.synchronizedList(new ArrayList<BuySell>());
    private static final List<BuySell> sellRequest = Collections.synchronizedList(new ArrayList<BuySell>());
    private static final Hashtable<String, User> allUsersTable = new Hashtable<>();
    //private static final List<User> allUsers = Collections.synchronizedList(new ArrayList<User>());
    private Logger logger = new Logger();

    private boolean marketState;

    private Map<String, Integer> stockTrendMap = new HashMap<>();
    private Map<String, Double> currentStockValues = new HashMap<>();
    private static double VARIANCE_FACTOR = 5;


    private void init() {
        //createStocks(StockType.values());
       // createStockTrendMap();
        //addDummyCurrentValues();
        User u1 =  returnUser(new Authentication("surag","surag"));
//    	User u2 =  returnUser(new Authentication("prateek","surag"));
//    	User u3 =  returnUser(new Authentication("nishant","surag"));
//    	addUser(u1);
//     	addUser(u2);
//    	addUser(u3);
        start();
    }

    @Override
    public void run() {
        while (true) {
        	//System.out.println("upadte market ---------------------------------------");

            if(marketState)
            {
                matchOrders();
                updateStockValues();
                List<User> usersList = getUserList();
                try {
                    logger.updateLogs(usersList,Market.getMarket());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.out.println("Market is On");
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private Market() {
        marketState = true;
        init();
    }

    public static Market getMarket() {
        if (marketInstance == null) {
            marketInstance = new Market();
        }
        return marketInstance;
    }

    public boolean startMarket() {
        marketState = true;
        return marketState;
    }


    public boolean stopMarket() {
        marketState = false;
        return marketState;
    }

    synchronized public boolean addStock(String name, double price, int qty) {
        Stock s = new Stock(name, price, qty);
        globalStocks.add(s);
        currentStockValues.put(name,price);
        propogateStock(s);
        return true;
    }
    
    synchronized public boolean propogateStock(Stock s) {
        User u = null;
        for(String i: allUsersTable.keySet())
        {
            u = allUsersTable.get(i);
            Portfolio p = u.getPortfolio();
            s.setStockQty(100);

            for(Stock stk: p.getStocks())
            {
                if(s.getStockName().equals(stk.getStockName()))
                {
                    return true;
                }
            }
            p.getStocks().add(s);


        }
        return true;
    }

    public boolean deleteStock(String name) {
        int len = globalStocks.size();
        boolean flag = false;
        for (Stock i : globalStocks) {
            String test = i.getStockName();
            if (test.equals(name)) {
                globalStocks.remove(i);
                flag = true;
                break;
            }
        }
        return flag;
    }

    public List<Stock> getMarketStocks() {
        return globalStocks;
    }

    public boolean getMarketState() {
        return marketState;
    }
    public List<User> getUserList()
    {
    	List<User> l = new ArrayList<>();
    	for (String i : allUsersTable.keySet()) {
			l.add(allUsersTable.get(i));
		}
        return l;
    }

    synchronized public boolean addBuyRequest(BuySell b) {
        buyRequest.add(b);
        return true;
    }

    synchronized public boolean addSellRequest(BuySell s) {
        sellRequest.add(s);
        return true;
    }

    synchronized public List<BuySell> getBuyRequest() {
        return buyRequest;
    }

    synchronized public List<BuySell> getSellRequest() {
        return sellRequest;
    }

    /**
     * Update Variance Factor
     * Update Portfolios
     * Update Transaction History
     */

    public void updateStockValues() {
        List<BuySell> buy = getBuyRequest();
        List<BuySell> sell = getSellRequest();
        int[] buyAmount = new int[globalStocks.size()];
        int[] sellAmount = new int[globalStocks.size()];
        double[] differences = new double[globalStocks.size()];
        int buyCnt = 0;
        int sellCnt = 0;
        for(Stock s: globalStocks)
        {
            
            for(BuySell b: buy)
            {
                if(s.getStockName().equals(b.getStockName().toLowerCase()))
                {
                    if(buyCnt<globalStocks.size())
                    {
                    buyAmount[buyCnt] += b.getQuantity();
                    
                    }
                }
            }
            for(BuySell b: sell)
            {
                if(s.getStockName().equals(b.getStockName().toLowerCase()))
                {
                    if(sellCnt<globalStocks.size())
                    {
                    sellAmount[sellCnt] +=b.getQuantity();
                    }
                }
            }
            buyCnt++;
            sellCnt++;
        }
        
        for(int i=0;i<globalStocks.size();i++)
        {
            differences[i] = (buyAmount[i] - sellAmount[i]);
            
            if(differences[i] > 0)
            {
                differences[i] = Math.log(differences[i]);
                
            }
            else if(differences[i] < 0 )
            {
                differences[i] = (Math.log(Math.abs(differences[i])))*-1;
            }
            differences[i] /=10;
            
        }
        
        int cnt = 0;
        for(Stock s: globalStocks)
        {
            double update = s.getUnitPrice() + differences[cnt];
            if(update < 0 ) update = 0;
            currentStockValues.put(s.getStockName(), update);
            s.setStockUnitPrice(update);
            cnt++;
        }

        
        
    }

    public Map<String, Double> getCurrentStockValues() {
        return this.currentStockValues;
    }


    private synchronized boolean matchOrders() {
        boolean atLeastOneMatch = false;
        Iterator<BuySell> i = buyRequest.iterator();
        Iterator<BuySell> j = sellRequest.iterator();
        while(i.hasNext()) {
            BuySell temp1 = i.next();
            while(j.hasNext()) {
                BuySell temp2 = j.next();
                //System.out.println("In Match: temp1" +temp1.getStockName() +" " +temp1.getQuantity() +" " +temp1.getUnitPrice() +" " +temp1.isBuy());
                //System.out.println("In Match: temp2" +temp2.getStockName() +" " +temp2.getQuantity() +" " +temp2.getUnitPrice() +" " +temp2.isBuy());
                if (temp1.getStockName().equals(temp2.getStockName())) {

                    if (temp1.getQuantity() == temp2.getQuantity()) {
                        System.out.println("found match" + temp1.getStockName());
           
                        updatePortfolio(temp1, temp2);
                        i.remove();
                        j.remove();
                        atLeastOneMatch = true;
                        break;
                    }
                }
            }
            if(atLeastOneMatch){
                atLeastOneMatch = false;
                break;
            }
        }
        return atLeastOneMatch;
    }

    public boolean addUser(User user) {
        if (allUsersTable.containsValue(user)) {
            //System.out.println("This user already exists");
            // log the same thing
            return false;

        } else {
            allUsersTable.put(user.getAuth().getUsername(), user);
            //System.out.println("in add user: " +user.getName());
        }
        return true;

    }

    private void updatePortfolio(BuySell b, BuySell s) {
        User ub = allUsersTable.get(b.getUserName());
       if (ub!=null) ub.getPortfolio().updatePortfolio(b);

        User sb = allUsersTable.get(s.getUserName());
        if (sb!=null)  sb.getPortfolio().updatePortfolio(s);
    }


    public User returnUser(Authentication authentication){
        List<User> users = getUserList();
        for (User u: users) {
            if(u.getAuth().getUsername().equals(authentication.getUsername())){
                return u;
            }
        }
        double balance = 88888;
        Set<Stock> stocks = new HashSet<>();
        stocks.add(new Stock("amazon",100,100));
        stocks.add(new Stock("google",100,100));
        stocks.add(new Stock("facebook",100,100));
        List<TransactionHistory> histories = new ArrayList<>();
        Portfolio p = new Portfolio(balance,stocks,histories);
        User nu = new User(authentication.getUsername(),"12345678","park central","6692426926",p,authentication);        
        addUser(nu);
        //System.out.println("Added User: " +nu.getAuth().getUsername());
        return nu;
    }

    public void createBuySell(String request)
    {	
    	//System.out.println("Request" +request);
        String[] req = request.split(",");
        
        BuySell b = new BuySell(req[0],Integer.parseInt(req[1]),req[2],Double.parseDouble(req[3]),Integer.parseInt(req[4]),Boolean.parseBoolean(req[5]));
//        System.out.println("OBJECT CREATED for BuySELL REQUEST");
//        System.out.println("Stock Name: "+b.getStockName());
//        System.out.println("User ID: "+b.getUserId());
//        System.out.println("User Name: "+b.getUserName());
//        System.out.println("Unit Price: "+b.getUnitPrice());
//        System.out.println("Quantity: "+b.getQuantity());
//        System.out.println("Is Buy? : "+b.isBuy());
        if(b.isBuy() == true)
        {	
        	//System.out.println("added buy");
            addBuyRequest(b);
        }
        else
        {	
        	//System.out.println("added sell");
            addSellRequest(b);
        }
    }




    public String parseStringCurrentStockValues(){
        Market m =  Market.getMarket();
        StringBuilder sb = new StringBuilder();
        Map<String,Double> vals =  m.getCurrentStockValues();
        for (String s: vals.keySet()
                ) {
            sb.append(s);
            sb.append(" : ");
            sb.append(vals.get(s));
            sb.append(",");
        }
       return  sb.toString();
    }


    public String getUserStringView(User user){
        StringBuilder sb = new StringBuilder();
        sb.append(parseStringCurrentStockValues());
        Set<Stock> s = user.getPortfolio().getStocks();
        for (Stock x:s) {
            sb.append(","+x.getStockName()+"qty:"+x.getStockQty());
        }
        sb.append(",balance:" + user.getPortfolio().getMoneyBalance());
        return sb.toString();
    }
    
    
    
    public String requestMarketUpdate(String username)
    {
        String result ="";
        for(Stock s: globalStocks)
        {
            String unitPrice = String.format("%.2f",s.getUnitPrice());
            result = result.concat(s.getStockName()+","+unitPrice+";");
        }
        result = result.concat("#");
        
        /*
        Create User Data String Below
        */
        
        User u = null;
        
        for(String i: allUsersTable.keySet())
        {
            if(allUsersTable.get(i).getAuth().getUsername().equals(username))
            {
                u = allUsersTable.get(i);
                break;
            }
        }
        String userData = "";
        Portfolio p = u.getPortfolio();
        Authentication a = u.getAuth();
        
        userData = userData.concat(userToString(u));
        userData = userData.concat(portfolioToString(p));
        userData = userData.concat(authToString(a));
        result = result.concat(userData);
        return result;
    }
    public String userToString(User u)
    {
        String userData = "";
        userData = userData.concat(u.getName()+","+u.getSSN()+","+u.getAddress()+","+u.getPhoneNumber()+"#");
        return userData;  
    }
    public String portfolioToString(Portfolio p)
    {
        String userData ="";
        String moneyBal = String.format("%.2f",p.getMoneyBalance());
        userData = userData.concat(moneyBal+",{");
        userData = userData.concat(stockListToString(p.getStocks()));
        userData = userData.concat("}#");
        return userData;
    }
    public String stockListToString(Set<Stock> userStocks)
    {
        String userData = ";";
        for(Stock s: userStocks)
        {
            userData = userData.concat(s.getStockName()+":"+s.getStockQty()+";");
        }
        return userData;
    }
    public String authToString(Authentication a)
    {
        String userData = "";
        userData = userData.concat(a.getUsername()+","+a.getPassword()+"#");
        return userData;
    }
//    ////////////////////////////////////////////////////////////////////
//    ///////////////////////////////ENCODE///////////////////////////////
//    public List<Stock> stringToMarket(String data)
//    {
//        List<Stock> marketStocks = new ArrayList<Stock>();
//        String[] arr = data.split("#");
//        String[] stocks = arr[0].split(";");
//
//        for(String s: stocks)
//        {
//            String[] values = s.split(",");
//            Stock stk = new Stock(values[0],Double.parseDouble(values[1]),0);
//            marketStocks.add(stk);
//        }
//        return marketStocks;
//    }
//    public User stringToUser(String data)
//    {
//        String[] fields = data.split("#");
//        String[] auth = fields[3].split(",");
//        Authentication a = new Authentication(auth[0],auth[1]);
//
//        String[] port = fields[2].split(",");
//        double moneyBalance = Double.parseDouble(port[0]);
//        port[1] = port[1].replace("{","");
//        port[1] = port[1].replace("}","");
//        String[] stocks = port[1].split(";");
//        Set<Stock> portStocks = new HashSet<Stock>();
//        for(String s: stocks)
//        {
//            if(s.equals("") == false)
//            {
//            String[] val = s.split(":");
//            Stock ob = new Stock(val[0],0,Integer.parseInt(val[1]));
//            portStocks.add(ob);
//            }
//        }
//        List<TransactionHistory> t = new ArrayList<TransactionHistory>();
//        Portfolio p = new Portfolio(moneyBalance,portStocks,t);
//
//        String[] userFields = fields[1].split(",");
//        User u = new User(userFields[0],userFields[1],userFields[2],userFields[3],p,a);
//        return u;
//
//    }
   
    
    


//    public static void main(String[] args) {
//        Market m = Market.getMarket();
//        m.createBuySell("amazon,1,sur,100,100,true");
//        //m.addSellRequest(new BuySell(StockType.AMAZON.toString().toLowerCase(), 1, "sur", 100, 100, false));
//        //m.addSellRequest(new BuySell(StockType.AMAZON.toString(),1,"sun",100, 100, false));
//        //m.matchOrders();
//       // m.updateStockValues();
//       // m.evaluateCurrentMarketValue();;
//        m.addStock("google",150,12);
//        m.addStock("facebook",200,12);
//
//            User u1 =  m.returnUser(new Authentication("surag","surag"));
//            User u2 =  m.returnUser(new Authentication("prateek","surag"));
//            User u3 =  m.returnUser(new Authentication("nishant","surag"));
//            m.addUser(u1);
//            m.addUser(u2);
//            m.addUser(u3);
//            String reqStr = m.requestMarketUpdate(u1.getAuth().getUsername());
//            System.out.println(reqStr);
//            List<Stock> markStock = m.stringToMarket(reqStr);
//            
//            
//            List<Stock> markStocks = m.stringToMarket(reqStr);
//            for(Stock s: markStocks)
//            {
//                System.out.println("Stock Name: "+s.getStockName()+","+"Stock price: "+s.getUnitPrice());
//            }
//            
//            
//
//            while (true){
//                
//            }
//
//    }
}
