package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.SessionSupplier;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubLoginRequest;

public final class SamsClubSessionProvider implements SessionSupplier<SamsClubLoginResponse> {
	
	private static SamsClubSessionProvider instance;
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Map<FulfillmentAccount, SamsClubLoginResponse> sessionCache = new HashMap<>();
	
	private SamsClubSessionProvider() {
		//singleton
	}
	
	public static synchronized SamsClubSessionProvider get() {
		if(instance == null) {
			instance = new SamsClubSessionProvider();
		}
		
		return instance;
	}

	@Override
	public SamsClubLoginResponse getSession(final FulfillmentAccount account, final WrappedHttpClient client) {
		lock.writeLock().lock();
		try {
			System.out.println("Getting current session for account: " + account);
			SamsClubLoginResponse currentSession = sessionCache.computeIfAbsent(account, acc -> null);
			if(currentSession == null) {
				final SamsClubLoginRequest request = new SamsClubLoginRequest(account, client);
				final Optional<JSONObject> response = request.execute();
				if(response.isPresent()) {
					final Map<String, String> cookies = request.getCookieMap();
					currentSession = new SamsClubLoginResponse(cookies, response.get());
					System.out.println("Caching current session for account " + account + ": " + currentSession);
					sessionCache.put(account, currentSession);
				} else {
					final String cookieStr = "CID=f74f13a0-ca73-49ba-9bf7-593be83bbbf5; JSESSIONID=BAC00D3C5431F144C2B9F89C9E6AA63B.estoreapp-659321024-18-728827627; NSC_JOqyreriep202r0eksinc1efkgrc2cc=48f2a3920faf3aa83788d6145e9d9610281afdd411fc2dd9b965e63f2e68fc0c5e848d69; SAT_AZURE_CLUB=0; SAT_EREXISTS=N; SAT_REACT_ALLOW_CLASSIC=1; SAT_REACT_LOGIN=2; SSID1=CABwyx0cAAAAAADyxC1eRhRDFvLELV4BAAAAAACe-Q5g8sQtXgBjBoHFAAPnHxsA8sQtXgEAv8UAAwwnGwDyxC1eAQA; SSLB=1; SSRT1=8sQtXgIAAA; SSSC1=362.G6786296757551830086.1|50561.1777639:50623.1779468; TS0139f7c0=0130aff23217a1b07ff151950ade1ac0c3db713db0bd4c2e23f54db8a85115b5babd73f010ee7eb726a3f0e4d0d2c076742fd33d39; TS01638ca2=0130aff23217a1b07ff151950ade1ac0c3db713db0bd4c2e23f54db8a85115b5babd73f010ee7eb726a3f0e4d0d2c076742fd33d39; TS0197017c=0130aff23217a1b07ff151950ade1ac0c3db713db0bd4c2e23f54db8a85115b5babd73f010ee7eb726a3f0e4d0d2c076742fd33d39; TS01f4281b=0130aff23217a1b07ff151950ade1ac0c3db713db0bd4c2e23f54db8a85115b5babd73f010ee7eb726a3f0e4d0d2c076742fd33d39; akavpau_P3=1580058448~id=ad74aa6718ed8a9a61662ed67b0f9420; bvUserToken=c9f5a5ba7213aa59ffe4c273dab34bac646174653d3230323030313236267573657269643d3430373934373938383035; cacn_v=fb0d2ca05b521a654d875650579d91eac6d14fa8583cdfae5e0d807826b1beb4; cdnOrigin=Y; dcenv=TB-DFW; esmno=fa193edc561852a6049d7c097390b960; firstNameCookie=Brendan; lgem=fb0d2ca05b521a654d875650579d91eac6d14fa8583cdfae5e0d807826b1beb4; memExpired=n; memType=savingsplus; membershipId=RHl3kN1q47oO2lXAGRbHCJZ/GjzV7tlEc/R/H4mUfKY=; mid=75aae7b8bc37b1a897d491d45fc5aaec295099f1afbee83a5234872979c695d8; myNeighboringClubs=8278|6463|8142|4950; myPreferredClub=4901; myPreferredClubName=Easley%2C+SC; pilotusercookie=memId%3A720b5edc556e99bb0abfdb3c211c1f2c860cb2fa23080a91aa7cb4fa3db55283%7Cplus%3AY; prdctc=0; prftdfp=5; prftdp=5; prftsl=5; samsHubbleSession=BAC00D3C5431F144C2B9F89C9E6AA63B.estoreapp-659321024-18-728827627; samsVisitor=24695091189; samsorder=95ff3510f2bf3a16b63669ee2beb1db6; signedIn=Y; ssl_token=7980963040CC8C70F868; tpLogin=true; tpLogintime=1580057842531; uExp=memId:720b5edc556e99bb0abfdb3c211c1f2c860cb2fa23080a91aa7cb4fa3db55283; usernameCookie=fb0d2ca05b521a654d875650579d91eac6d14fa8583cdfae5e0d807826b1beb4;";
					final Map<String, String> cookieMap = WrappedHttpClient.generateCookieMapFromString(cookieStr);
					currentSession = new SamsClubLoginResponse(cookieMap, null);
					sessionCache.put(account, currentSession);
					client.setCookies("samsclub.com", "/", cookieMap);
				}
			}
			
			return currentSession;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void clearSession(final FulfillmentAccount account) {
		lock.writeLock().lock();
		try {
			System.out.println("Clearing session for account: " + account);
			sessionCache.put(account, null);
		} finally {
			lock.writeLock().unlock();
		}
	}

}
