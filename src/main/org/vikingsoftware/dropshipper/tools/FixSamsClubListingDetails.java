package main.org.vikingsoftware.dropshipper.tools;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.call.ReviseFixedPriceItemCall;
import com.ebay.soap.eBLBaseComponents.BrandMPNType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.PictureDetailsType;
import com.ebay.soap.eBLBaseComponents.ProductListingDetailsType;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayApiContextManager;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsProductAPI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingImage;

public final class FixSamsClubListingDetails {

	public static void main(final String[] args) {
		final ExecutorService executor = Executors.newFixedThreadPool(8);
		final List<FulfillmentListing> listings = FulfillmentManager.get().getListingsForFulfillmentPlatform(FulfillmentPlatforms.SAMS_CLUB);
		final ApiContext ebayApi = EbayApiContextManager.getLiveContext();
		for(final FulfillmentListing listing : listings) {
			executor.execute(() -> {
				String upcStr = null;
				String eanStr = null;
				String marketListingId = null;
				try {
					final FulfillmentListing list = listing;
					final SamsProductAPI api = new SamsProductAPI();
					final MarketplaceListing marketListing = MarketplaceListing.getMarketplaceListingForFulfillmentListing(list.id);
					if(marketListing == null) {
						return;
					}
					marketListingId = marketListing.listingId;
					//System.out.println("Starting image update process for listing " + list.id);
					api.parse(list.product_id);
					
					List<ListingImage> images = api.getImages();
					if(images.size() > 8) {
						images = images.subList(0, 8);
					}
					final ReviseFixedPriceItemCall call = new ReviseFixedPriceItemCall(ebayApi);
					final ItemType itemToRevise = new ItemType();
					itemToRevise.setItemID(marketListing.listingId);
					itemToRevise.setPictureDetails(createPictureDetailsForListing(images));
					
					final ProductListingDetailsType productListingDetails = new ProductListingDetailsType();
					productListingDetails.setIncludeeBayProductDetails(true);
					productListingDetails.setIncludeStockPhotoURL(false);
					productListingDetails.setUseStockPhotoURLAsGallery(false);
					productListingDetails.setUseFirstProduct(true);
					
					final Optional<String> upc = api.getUPC();
					if(upc.isPresent()) {
						productListingDetails.setUPC(upc.get());
					} else {
						productListingDetails.setUPC("Does not Apply");
					}
					
					final Optional<String> ean = api.getEAN();
					if(ean.isPresent()) {
						productListingDetails.setEAN(ean.get());
					} else {
						productListingDetails.setEAN("Does not Apply");
					}
					
					upcStr = productListingDetails.getUPC();
					eanStr = productListingDetails.getEAN();
					
					
					final BrandMPNType brandMpn = new BrandMPNType();
					
					final Optional<String> brandName = api.getBrandName();
					if(brandName.isPresent()) {
						brandMpn.setBrand(brandName.get());
					} else {
						brandMpn.setBrand("Unbranded");
					}
					
					final Optional<String> modelNumber = api.getModelNumber();
					if(modelNumber.isPresent()) {
						brandMpn.setMPN(modelNumber.get());
					} else {
						brandMpn.setMPN("Does not Apply");
					}
					
					productListingDetails.setBrandMPN(brandMpn);
					
					itemToRevise.setProductListingDetails(productListingDetails);
					call.setItemToBeRevised(itemToRevise);
					call.reviseFixedPriceItem();
					System.out.println("Successfully updated images for listing https://www.ebay.com/itm/" + marketListingId + ", upc: " + upcStr + ", ean: " + eanStr);
					
				} catch(final Exception e) {
					System.err.println("Failed to update info for listing https://www.ebay.com/itm/" + marketListingId + ", upc: " + upcStr + ", ean: " + eanStr);
					e.printStackTrace();			
				}
			});
		}
	}
	
	private static PictureDetailsType createPictureDetailsForListing(final List<ListingImage> images) {
		final PictureDetailsType type = new PictureDetailsType();
		final String[] urls = images.stream()
				.map(image -> image.url.replace("http://", "https://")) //eBay requires external images to have a link with https
				.toArray(String[]::new);

		type.setExternalPictureURL(urls);
		return type;
	}
	
	
}
