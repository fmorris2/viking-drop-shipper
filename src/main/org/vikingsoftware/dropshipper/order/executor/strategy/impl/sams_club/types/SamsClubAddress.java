package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubListAddressesRequest;

public class SamsClubAddress {
	
	public final String addressId;
	public final String addressType;
	public String firstName;
	public final String lastName;
	public final String middleName;
	public final String addressLineOne;
	public final String city;
	public final String prefix;
	public final String suffix;
	public final String stateOrProvinceCode;
	public final String postalCode;
	public final String countryCode;
	public final String phone;
	public final String phoneNumberType;
	public final String phoneTwoType;
	public final String phoneTwo;
	public final String nickName;
	public final String addressLineTwo;
	public final String addressLineThree;
	public final String businessName;
	public final boolean isDefault;
	public final boolean dockDoorPresent;
	
	private SamsClubAddress(final Builder builder) {
		this.addressId = builder.addressId;
		this.addressType = builder.addressType;
		this.firstName = builder.firstName;
		this.lastName = builder.lastName;
		this.middleName = builder.middleName;
		this.addressLineOne = builder.addressLineOne;
		this.city = builder.city;
		this.prefix = builder.prefix;
		this.suffix = builder.suffix;
		this.stateOrProvinceCode = builder.stateOrProvinceCode;
		this.postalCode = builder.postalCode;
		this.countryCode = builder.countryCode;
		this.phone = builder.phone;
		this.phoneNumberType = builder.phoneNumberType;
		this.phoneTwoType = builder.phoneTwoType;
		this.phoneTwo = builder.phoneTwo;
		this.nickName = builder.nickName;
		this.addressLineTwo = builder.addressLineTwo;
		this.addressLineThree = builder.addressLineThree;
		this.dockDoorPresent = builder.dockDoorPresent;
		this.businessName = builder.businessName;
		this.isDefault = builder.isDefault;
	}
	
	public static Optional<SamsClubAddress> findDefaultAddress(final WrappedHttpClient client) {
		
		final SamsClubListAddressesRequest req = new SamsClubListAddressesRequest(client);
		final List<SamsClubAddress> addresses = req.execute();
		
		return Optional.ofNullable(addresses.stream()
				.filter(addr -> addr.isDefault)
				.findFirst()
				.orElse(null)
		);
	}
	
	public void updateJSONObject(final JSONObject json) {
		json.put("addressId", this.addressId);
		json.put("addressType", this.addressType);
		json.put("firstName", this.firstName);
		json.put("lastName", this.lastName);
		json.put("address1", this.addressLineOne);
		json.put("address2", this.addressLineTwo);
		json.put("city", this.city);
		json.put("state", this.stateOrProvinceCode);
		json.put("postalCode", this.postalCode);
		json.put("country", this.countryCode);
		json.put("phoneAreaCode", "916");
		json.put("phonePrefix", "245");
		json.put("phoneSuffix", "0125");
	}
	
	@Override
	public String toString() {
		return "\tSamsClubAddress:\n"
				+ "\t\tAddress ID: " + addressId + "\n"
				+ "\t\tAddress Type: " + addressType + "\n"
				+ "\t\tFirst Name: " + firstName + "\n"
				+ "\t\tMiddle Name: " + middleName + "\n"
				+ "\t\tLast Name: " + lastName + "\n"
				+ "\t\tAddress Line One: " + addressLineOne + "\n"
				+ "\t\tAddress Line Two: " + addressLineTwo + "\n"
				+ "\t\tAddress Line Three: " + addressLineThree + "\n"
				+ "\t\tCity: " + city + "\n"
				+ "\t\tState: " + stateOrProvinceCode + "\n"
				+ "\t\tPostal Code: " + postalCode + "\n"
				+ "\t\tCountry Code: " + countryCode + "\n"
				+ "\t\tPhone: " + phone + "\n";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addressId == null) ? 0 : addressId.hashCode());
		result = prime * result + ((addressLineOne == null) ? 0 : addressLineOne.hashCode());
		result = prime * result + ((addressLineThree == null) ? 0 : addressLineThree.hashCode());
		result = prime * result + ((addressLineTwo == null) ? 0 : addressLineTwo.hashCode());
		result = prime * result + ((addressType == null) ? 0 : addressType.hashCode());
		result = prime * result + ((businessName == null) ? 0 : businessName.hashCode());
		result = prime * result + ((city == null) ? 0 : city.hashCode());
		result = prime * result + ((countryCode == null) ? 0 : countryCode.hashCode());
		result = prime * result + (dockDoorPresent ? 1231 : 1237);
		result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result + (isDefault ? 1231 : 1237);
		result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
		result = prime * result + ((middleName == null) ? 0 : middleName.hashCode());
		result = prime * result + ((nickName == null) ? 0 : nickName.hashCode());
		result = prime * result + ((phone == null) ? 0 : phone.hashCode());
		result = prime * result + ((phoneNumberType == null) ? 0 : phoneNumberType.hashCode());
		result = prime * result + ((phoneTwo == null) ? 0 : phoneTwo.hashCode());
		result = prime * result + ((phoneTwoType == null) ? 0 : phoneTwoType.hashCode());
		result = prime * result + ((postalCode == null) ? 0 : postalCode.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		result = prime * result + ((stateOrProvinceCode == null) ? 0 : stateOrProvinceCode.hashCode());
		result = prime * result + ((suffix == null) ? 0 : suffix.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SamsClubAddress other = (SamsClubAddress) obj;
		if (addressId == null) {
			if (other.addressId != null)
				return false;
		} else if (!addressId.equalsIgnoreCase(other.addressId))
			return false;
		if (addressLineOne == null) {
			if (other.addressLineOne != null)
				return false;
		} else if (!addressLineOne.equalsIgnoreCase(other.addressLineOne))
			return false;
		if (addressLineThree == null) {
			if (other.addressLineThree != null)
				return false;
		} else if (!addressLineThree.equalsIgnoreCase(other.addressLineThree))
			return false;
		if (addressLineTwo == null) {
			if (other.addressLineTwo != null)
				return false;
		} else if (!addressLineTwo.equalsIgnoreCase(other.addressLineTwo))
			return false;
		if (addressType == null) {
			if (other.addressType != null)
				return false;
		} else if (!addressType.equalsIgnoreCase(other.addressType))
			return false;
		if (businessName == null) {
			if (other.businessName != null)
				return false;
		} else if (!businessName.equalsIgnoreCase(other.businessName))
			return false;
		if (city == null) {
			if (other.city != null)
				return false;
		} else if (!city.equalsIgnoreCase(other.city))
			return false;
		if (countryCode == null) {
			if (other.countryCode != null)
				return false;
		} else if (!countryCode.equalsIgnoreCase(other.countryCode))
			return false;
		if (dockDoorPresent != other.dockDoorPresent)
			return false;
		if (firstName == null) {
			if (other.firstName != null)
				return false;
		} else if (!firstName.equalsIgnoreCase(other.firstName))
			return false;
		if (isDefault != other.isDefault)
			return false;
		if (lastName == null) {
			if (other.lastName != null)
				return false;
		} else if (!lastName.equalsIgnoreCase(other.lastName))
			return false;
		if (middleName == null) {
			if (other.middleName != null)
				return false;
		} else if (!middleName.equalsIgnoreCase(other.middleName))
			return false;
		if (nickName == null) {
			if (other.nickName != null)
				return false;
		} else if (!nickName.equalsIgnoreCase(other.nickName))
			return false;
		if (phone == null) {
			if (other.phone != null)
				return false;
		} else if (!phone.equalsIgnoreCase(other.phone))
			return false;
		if (phoneNumberType == null) {
			if (other.phoneNumberType != null)
				return false;
		} else if (!phoneNumberType.equalsIgnoreCase(other.phoneNumberType))
			return false;
		if (phoneTwo == null) {
			if (other.phoneTwo != null)
				return false;
		} else if (!phoneTwo.equalsIgnoreCase(other.phoneTwo))
			return false;
		if (phoneTwoType == null) {
			if (other.phoneTwoType != null)
				return false;
		} else if (!phoneTwoType.equalsIgnoreCase(other.phoneTwoType))
			return false;
		if (postalCode == null) {
			if (other.postalCode != null)
				return false;
		} else if (!postalCode.equalsIgnoreCase(other.postalCode))
			return false;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equalsIgnoreCase(other.prefix))
			return false;
		if (stateOrProvinceCode == null) {
			if (other.stateOrProvinceCode != null)
				return false;
		} else if (!stateOrProvinceCode.equalsIgnoreCase(other.stateOrProvinceCode))
			return false;
		if (suffix == null) {
			if (other.suffix != null)
				return false;
		} else if (!suffix.equalsIgnoreCase(other.suffix))
			return false;
		return true;
	}
	
	public static final class Builder {
		private String addressId = "";
		private String addressType = "Residential";
		private String firstName = "";
		private String lastName = "";
		private String middleName = "";
		private String addressLineOne = "";
		private String city = "";
		private String prefix = "";
		private String suffix = "";
		private String stateOrProvinceCode = "";
		private String postalCode = "";
		private String countryCode = "";
		private String phone = "";
		private String phoneNumberType = "";
		private String phoneTwoType = "";
		private String phoneTwo = "";
		private String nickName = "";
		private String addressLineTwo = "";
		private String addressLineThree = "";
		private String businessName = "";
		private boolean isDefault = false;
		private boolean dockDoorPresent = false;
		
		public Builder addressId(final String addressId) {
			this.addressId = addressId;
			return this;
		}
		
		public Builder addressType(final String addressType) {
			this.addressType = addressType;
			return this;
		}
		
		public Builder firstName(final String val) {
			this.firstName = val;
			return this;
		}
		
		public Builder lastName(final String val) {
			this.lastName = val;
			return this;
		}
		
		public Builder middleName(final String val) {
			this.middleName = val;
			return this;
		}
		
		public Builder addressLineOne(final String val) {
			this.addressLineOne = val;
			return this;
		}
		
		public Builder city(final String val) {
			this.city = val;
			return this;
		}
		
		public Builder prefix(final String val) {
			this.prefix = val;
			return this;
		}

		public Builder suffix(final String val) {
			this.suffix = val;
			return this;
		}
		
		public Builder stateOrProvinceCode(final String val) {
			this.stateOrProvinceCode = val;
			return this;
		}
		
		public Builder postalCode(final String val) {
			this.postalCode = val;
			return this;
		}
		
		public Builder countryCode(final String val) {
			this.countryCode = val;
			return this;
		}
		
		public Builder phone(final String val) {
			this.phone = val;
			return this;
		}
		
		public Builder phoneNumberType(final String val) {
			this.phoneNumberType = val;
			return this;
		}
		
		public Builder phoneTwoType(final String val) {
			this.phoneTwoType = val;
			return this;
		}
		
		public Builder phoneTwo(final String val) {
			this.phoneTwo = val;
			return this;
		}
		
		public Builder nickName(final String val) {
			this.nickName = val;
			return this;
		}
		
		public Builder addressLineTwo(final String val) {
			this.addressLineTwo = val;
			return this;
		}
		
		public Builder addressLineThree(final String val) {
			this.addressLineThree = val;
			return this;
		}
		
		public Builder dockDoorPresent(final boolean val) {
			this.dockDoorPresent = val;
			return this;
		}
		
		public Builder businessName(final String val) {
			this.businessName = val;
			return this;
		}
		
		public Builder isDefault(final boolean val) {
			this.isDefault = val;
			return this;
		}
		
		public SamsClubAddress build() {
			return new SamsClubAddress(this);
		}
		
	}
}
