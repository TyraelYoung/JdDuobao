package in.tyreal.raider.net;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import in.tyrael.raider.RaiderUtil;
import in.tyrael.raider.bean.AuctionBean;
import in.tyrael.raider.bean.BidBean;
import in.tyrael.raider.bean.CommodityBean;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 实现访问网站功能
 * 
 * 自己管理cookie字符串
 * 
 * 处理具体业务逻辑 1. 收发报文 2. 解析html，获取想要的数据
 * 
 * TODO 设计成单例模式，作为应用的浏览器代理。
 * 
 * 为方便起见，一部分功能用webview实现。
 * 
 * 如果想把生命周期实现为整个应用，是否应该作为服务运行？
 * 
 * 是否设计成单例模式比较好？
 * 
 * TODO 考虑同步
 * 
 * 静态函数不用登陆
 * 
 */
public class RaiderHttpAgent {
	private final Logger log = Logger.getLogger(RaiderHttpAgent.class);
	
	private static RaiderHttpAgent raiderAgent;
	
	//登陆以后，该单例对象保存bidservice中		
	//状态变量
	//类上加锁
	//只有这个状态变量是有用的
	//整个个应用应该之用一个cookie
	private static String cookie;
	
	public synchronized void setCookie(String strCookie) {
		RaiderHttpAgent.cookie = strCookie;		
	}

	private RaiderHttpAgent() {
	}
	
	public static synchronized RaiderHttpAgent getInstance() {
		if (raiderAgent == null) {
			raiderAgent = new RaiderHttpAgent();
			//httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY); 			
		}
		return raiderAgent;
	}
	
	public boolean isLogin() {		
		return true;
	}

	// 登陆是异步的？
	//用广播器，启动一个activity登陆？
	public void login() {

	}
	
	//出价
	public void bid(AuctionBean ab,  int price){
		// TODO
		if (!isLogin()) {
			login();
		}
		String url = RaiderHttpUtil.URL_BID + "dealId=" + ab.getJdId() + "&price=" + price/100;
		RaiderHttpUtil.getHtml(url, cookie);
		
		//日志
		log.info("已发送bid" + RaiderUtil.jdDateFormat.format(new Date(System.currentTimeMillis())));
		log.info(ab.getCommodity().getName() + "：	出价请求已发送，价格：" + price/100);		
	}
	
//	auctionStatus: null
//	datas: [,…]
//	0: {id:17974796, paimaiDealId:3988052, productId:807028, userNickName:v0ak69ofrp9, bidTime:1384940219948,…}
//	bidStatus: null
//	bidTime: 1384940219948
//	id: 17974796
//	ipAddress: "111.206.78.148"
//	paimaiDealId: 3988052
//	price: 773
//	productId: 807028
//	userNickName: "v0ak69ofrp9"
//	1: {id:17974789, paimaiDealId:3988052, productId:807028, userNickName:5257435856, bidTime:1384940218586,…}
//	2: {id:17974786, paimaiDealId:3988052, productId:807028, userNickName:kan_1984, bidTime:1384940217343,…}
//	3: {id:17974763, paimaiDealId:3988052, productId:807028, userNickName:孙建材88, bidTime:1384940195479,…}
//	4: {id:17974719, paimaiDealId:3988052, productId:807028, userNickName:sile_waj, bidTime:1384940154446,…}
//	5: {id:17971254, paimaiDealId:3988052, productId:807028, userNickName:1179103010_m, bidTime:1384936597815,…}
//	6: {id:17970639, paimaiDealId:3988052, productId:807028, userNickName:lujinlong566, bidTime:1384936060813,…}
//	7: {id:17970236, paimaiDealId:3988052, productId:807028, userNickName:jd_719de7f2d7824,…}
//	pageNo: 1
//	pageSize: 8
//	totalItem: 10
//	totalPages: 2
//	trxBuyerName: "v0ak69ofrp9"
//	trxPrice: 773
	
//"id":17970639,"paimaiDealId":3988052,"productId":807028,"userNickName":"lujinlong566","bidTime":1384936060813,"price":17.0,"ipAddress":"60.6.210.241","bidStatus":null}	
	
	
	public List<BidBean> getBid(AuctionBean ab){
		if(ab == null || ab.getJdId() == 0){
			throw new RuntimeException("商品id不能为空");
		}
		
		//返回json
		String html = RaiderHttpUtil.getHtml(
				RaiderHttpUtil.URL_GET_BID + ab.getJdId(),
				cookie);
		//log.info("价格网页解析开始" + RaiderUtil.bidDateFormat.format(new Date(System.currentTimeMillis())));
		if (html != null && !"".equals(html)) {
			List<BidBean> lbb = new ArrayList<BidBean>();
			try {
				JSONObject joBidRecord = new JSONObject(html);
				JSONArray joData = joBidRecord.getJSONArray("datas");
				for(int i =0; i <joData.length(); i++){
					JSONObject joBid = joData.getJSONObject(i);
					BidBean bb = new BidBean();
					bb.setJdId(joBid.getInt("id"));
					bb.setUserName(joBid.getString("userNickName"));
					bb.setTime(joBid.getLong("bidTime"));
					bb.setPrice((int) (joBid.getDouble("price") * 100));
					lbb.add(bb);
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return lbb;

		}else{
			return null;
		}
		
	}

	/**
	 * 获取commodity详细信息
	 * TODO 目前是通过解析auction详情页面获取商品信息,必须有auction的id
	 * 
	 * auction详情页面

	 * @return
	 */
	public CommodityBean getCommodityByAuction(CommodityBean cb, AuctionBean ab) {
		if (cb == null || ab == null || ab.getCommodity() == null) {
			throw new RuntimeException("商品不能为空");
		}
		String html = RaiderHttpUtil.getHtml(
				RaiderHttpUtil.URL_AUCTION_DETAIL + ab.getJdId(),
				cookie);
		if (html != null && !"".equals(html)) {
			Document doc = Jsoup.parse(html);
			Elements esJdId = doc.select("#aprinfo");
			String str = esJdId.get(0).attr("data-url");///json/paimai/productDesciption?productId=713714
			String strJdId = str.substring(str.indexOf("=") + 1);
			int jdId = Integer.valueOf(strJdId);
			cb.setJdId(jdId);
			ab.getCommodity().setJdId(jdId);
			
			Elements esPrice = doc.select("#product-intro").select("del");
			String strp = esPrice.text();
			strp = strp.substring(1, strp.length());// ￥679.00
			cb.setJdPrice((int) (Float.valueOf(strp) * 100));// 单位分
			ab.getCommodity().setJdPrice((int) (Float.valueOf(strp) * 100));
			return cb;

		}else{
			throw new RuntimeException("html为空");
		}
	}

	/**
	 * 返回我的夺宝箱，解析加入数据库
	 */
	public List<AuctionBean> getMyAuction() {
		// TODO
		if (!isLogin()) {

		}
		List<AuctionBean> lab = new ArrayList<AuctionBean>();
		String htmlMyAuction = RaiderHttpUtil.getHtml(
				RaiderHttpUtil.URL_MY_AUCTION, cookie);
		// 解析 TODO
		if (htmlMyAuction != null && !"".equals(htmlMyAuction)) {
			Document doc = Jsoup.parse(htmlMyAuction);
			Elements trs = doc.select("table").select("tr");
			for (int i = 0; i < trs.size(); i++) {
				Elements tds = trs.get(i).select("td");

				// Log.d("AgentSize", String.valueOf(tds.size()));
				// 注意跳过表头,表头的size为0
				if (tds.size() > 1) {
					Element td0 = tds.get(0);
					Elements imgs = tds.get(0).select("img");

					// 1. 查看商品是否存在
					CommodityBean cb = new CommodityBean();
					cb.setName(tds.get(0).select("img").get(0).attr("title"));

					// 2. 创建Auction对象。
					AuctionBean ab = new AuctionBean();
					

					ab.setJdId(Integer.valueOf(tds.get(1).text()));
					ab.setCommodity(cb);
					ab.setCommodity(getCommodityByAuction(cb, ab));
					
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm");
					try {
						ab.setStartTime(sdf.parse(tds.get(4).text()).getTime());
						ab.setEndTime(sdf.parse(tds.get(5).text()).getTime());
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				

					lab.add(ab);
				}

			}
		}
		return lab;
	}
	
	/*
	 * 返回参数与服务器时间的差。
	 * {"now":1393255446597}
	 */
	public static long elapsedTime(long future){
		String json = RaiderHttpUtil.getHtml(RaiderHttpUtil.URL_SERVER_TIME);
		long nowServer = Long.valueOf(json.substring(7, 20));
		return future - nowServer;
	}
}