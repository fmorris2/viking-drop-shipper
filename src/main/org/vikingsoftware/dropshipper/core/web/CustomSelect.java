package main.org.vikingsoftware.dropshipper.core.web;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class CustomSelect extends Select {

	public CustomSelect(final WebElement element) {
		super(element);
	}
	
	public Optional<WebElement> findOptionByCaseInsensitiveValue(final String value) {
		try {
			final Field field = getClass().getSuperclass().getDeclaredField("element");
			field.setAccessible(true);
			final WebElement element = (WebElement)field.get(this);
			
			final List<WebElement> options = element.findElements(
					By.xpath(".//option[translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"+value.toLowerCase()+"']"));
			if (options.isEmpty()) {
				throw new NoSuchElementException("Cannot locate option with value: " + value);
			}
			return Optional.of(options.get(0));
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to findOptionByCaseInsentiveValue("+value+"): ", e);
		}
		
		return Optional.empty();
	}
	
}
