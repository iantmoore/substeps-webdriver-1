/*
 *	Copyright Technophobia Ltd 2012
 *
 *   This file is part of Substeps.
 *
 *    Substeps is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    Substeps is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with Substeps.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.technophobia.webdriver.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitWebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.technophobia.substeps.step.StepImplementationUtils;

/**
 * @author imoore
 */
public abstract class WebDriverSubstepsBy {

    

    public static ByIdAndText ByIdAndText(final String id, final String text) {
        return new ByIdAndText(id, text);
    }

    public static ByIdAndText ByIdAndCaseSensitiveText(final String id, final String text) {
        return new ByIdAndText(id, text, true);
    }

    public static ByTagAndAttributes ByTagAndAttributes(final String tagName,
            final Map<String, String> requiredAttributes) {
        return new ByTagAndAttributes(tagName, requiredAttributes);
    }

    public static ByTagAndAttributes ByTagAndAttributes(final String tagName, final String attributeString) {

        final Map<String, String> expectedAttributes = StepImplementationUtils.convertToMap(attributeString);

        return new ByTagAndAttributes(tagName, expectedAttributes);
    }

    public static ByTagAndAttributes NthByTagAndAttributes(final String tagName, final String attributeString,
            final int nth) {

        final Map<String, String> expectedAttributes = StepImplementationUtils.convertToMap(attributeString);

        return new ByTagAndAttributes(tagName, expectedAttributes, nth);
    }

    public static ByCurrentWebElement ByCurrentWebElement(final WebElement elem) {
        return new ByCurrentWebElement(elem);
    }

    public static ByTagAndWithText ByTagAndWithText(final String tag, final String text) {
        return new ByTagAndWithText(tag, text);
    }

    public static ByTagAndWithText ByTagContainingText(final String tag, final String text) {
        return new ByTagAndContainingText(tag, text);
    }

    public static ByTagAndWithText ByTagStartingWithText(final String tag, final String text) {
        return new ByTagAndStartingWithText(tag, text);
    }

    public static ByIdContainingText ByIdContainingText(final String id, final String text) {
        return new ByIdContainingText(id, text);
    }

    public static BySomethingContainingText ByXpathContainingText(final String xpath, final String text) {
        return new BySomethingContainingText(By.xpath(xpath), text);
    }

    public static abstract class BaseBy extends By {

        private static Logger logger = LoggerFactory.getLogger(BaseBy.class);

        @Override
        public final List<WebElement> findElements(final SearchContext context) {

            List<WebElement> matchingElems = null;
            try {
                matchingElems = findElementsBy(context);
            }
            catch (StaleElementReferenceException e){
                logger.debug("StaleElementReferenceException looking for elements");
            }

            // NB. returning non null will prevent any wait from waiting.. HTML unit is a bit tricky in this respect as it expects an empty collection,
            // not compatible with any waits...

            if (matchingElems == null && context instanceof HtmlUnitWebElement) {
                matchingElems = Collections.EMPTY_LIST;
            }
            return matchingElems;
        }

        public abstract List<WebElement> findElementsBy(final SearchContext context);
    }

    public static abstract class XPathBy extends BaseBy {

        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            final StringBuilder xpathBuilder = new StringBuilder();

            buildXPath(xpathBuilder);

            return context.findElements(By.xpath(xpathBuilder.toString()));
        }

        protected abstract void buildXPath(StringBuilder xpathBuilder);
    }

    static class ByTagAndAttributes extends XPathBy {

        private static final Logger logger = LoggerFactory.getLogger(ByTagAndAttributes.class);
        
        private final String tagName;
        private final Map<String, String> requiredAttributes;
        private final int minimumExpected;

        ByTagAndAttributes(final String tagName, final Map<String, String> requiredAttributes) {
            this.tagName = tagName;
            this.requiredAttributes = requiredAttributes;
            this.minimumExpected = 1;
        }

        ByTagAndAttributes(final String tagName, final Map<String, String> requiredAttributes, final int nth) {
            this.tagName = tagName;
            this.requiredAttributes = requiredAttributes;
            this.minimumExpected = nth;
        }

        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(tagName);

            if (!requiredAttributes.isEmpty()) {
                xpathBuilder.append("[");

                boolean firstOne = true;

                for (final Map.Entry<String, String> requiredAttribute : requiredAttributes.entrySet()) {

                    if (!firstOne) {
                        xpathBuilder.append(" and ");
                    }

                    xpathBuilder.append("@").append(requiredAttribute.getKey()).append(" = '")
                            .append(requiredAttribute.getValue()).append("'");

                    firstOne = false;
                }

                xpathBuilder.append("]");
            }

        }

        @Override
        public List<WebElement> findElementsBy(final SearchContext searchContext) {

            final List<WebElement> matchingElems = super.findElementsBy(searchContext);

            if (matchingElems != null && matchingElems.size() < this.minimumExpected) {
                logger.info("expecting at least " + this.minimumExpected + " matching elems, found only "
                        + matchingElems.size() + " this time around");
                // we haven't found enough, clear out

                return null;
            }

            return matchingElems;
        }
    }

    /**
     * A By for use with the current web element, to be chained with other Bys
     */
    static class ByCurrentWebElement extends BaseBy {

        private final WebElement currentElement;

        public ByCurrentWebElement(final WebElement elem) {
            this.currentElement = elem;
        }

        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            final List<WebElement> matchingElems = new ArrayList<WebElement>();
            matchingElems.add(this.currentElement);

            return matchingElems;
        }
    }

    static class ByTagAndWithText extends XPathBy {

        protected final String tag;
        protected final String text;

        ByTagAndWithText(final String tag, final String text) {
            this.tag = tag;
            this.text = text;
        }

        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(this.tag).append("[")
                    .append(equalsIgnoringCaseXPath("text()", "'" + this.text.toLowerCase() + "'")).append("]");
        }

    }

    static class ByTagAndContainingText extends ByTagAndWithText {

        ByTagAndContainingText(final String tag, final String text) {
            super(tag, text);
        }

        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(this.tag).append("[contains(text(), '").append(this.text).append("')]");
        }
    }

    static class ByTagAndStartingWithText extends ByTagAndWithText {

        ByTagAndStartingWithText(final String tag, final String text) {
            super(tag, text);
        }

        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(this.tag).append("[starts-with(text(), '").append(this.text)
                    .append("')]");
        }
    }

    static class BySomethingContainingText extends BaseBy {

        protected final String text;
        protected final By by;

        BySomethingContainingText(final By by, final String text) {
            this.by = by;
            this.text = text;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.openqa.selenium.By#findElements(org.openqa.selenium.SearchContext
         * )
         */
        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            List<WebElement> matchingElems = null;

            final List<WebElement> elems = context.findElements(this.by);
            if (elems != null) {
                for (final WebElement e : elems) {

                    if (e.getText() != null && e.getText().contains(this.text)) {

                        if (matchingElems == null) {
                            matchingElems = new ArrayList<WebElement>();
                        }
                        matchingElems.add(e);
                    }
                }
            }

            return matchingElems;
        }
    }

    static class ByIdContainingText extends XPathBy {

        protected final String text;
        protected final String id;

        ByIdContainingText(final String id, final String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {

            xpathBuilder.append(".//*[@id='").append(this.id).append("' and contains(text(), '").append(this.text)
                    .append("')]");

        }
    }

    public static class ByIdAndText extends XPathBy {

        protected final String text;
        protected final String id;
        protected final boolean caseSensitive;

        ByIdAndText(final String id, final String text) {
            this(id, text, false);
        }

        ByIdAndText(final String id, final String text, final boolean caseSensitive) {
            this.id = id;
            this.text = text;
            this.caseSensitive = caseSensitive;
        }

        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {

            xpathBuilder.append(".//*[@id='").append(this.id).append("' and ");

            if (caseSensitive) {
                xpathBuilder.append("text()='").append(this.text).append("'");
            } else {
                xpathBuilder.append(equalsIgnoringCaseXPath("text()", "'" + this.text.toLowerCase() + "'"));
            }

            xpathBuilder.append("]");
        }
    }

    private static String equalsIgnoringCaseXPath(final String str1, final String str2) {
        return new StringBuilder().append("translate(").append(str1)
                .append(", 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')=").append(str2).toString();
    }
    
    
    public static ByTagAndAttributesWithValue ByTagAndAttributesWithValue(final String tagName,
            final String attributeString, final String value) {

        final Map<String, String> expectedAttributes = StepImplementationUtils.convertToMap(attributeString);

        return new ByTagAndAttributesWithValue(tagName, expectedAttributes, value);
    }

    public static ByTagAndAttributesWithText ByTagAndAttributesWithText(final String tagName,
            final String attributeString, final String text) {

        final Map<String, String> expectedAttributes = StepImplementationUtils.convertToMap(attributeString);

        return new ByTagAndAttributesWithText(tagName, expectedAttributes, text);
    }

    static class ByTagAndAttributesWithText extends XPathBy {

        private static final Logger logger = LoggerFactory.getLogger(ByTagAndAttributesWithText.class);

        private final String tagName;
        private final Map<String, String> requiredAttributes;
        private final String text;
        private final int minimumExpected;

        ByTagAndAttributesWithText(final String tagName, final Map<String, String> requiredAttributes, final String text) {
            this(tagName, requiredAttributes, text, 1);
        }

        ByTagAndAttributesWithText(final String tagName, final Map<String, String> requiredAttributes,
                final String text, final int nth) {
            this.tagName = tagName;
            this.requiredAttributes = requiredAttributes;
            this.text = text;
            this.minimumExpected = nth;
        }

        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(tagName);

            final boolean hasAttributes = !requiredAttributes.isEmpty();
            final boolean hasText = StringUtils.isNotEmpty(text);

            if (hasAttributes || hasText) {
                xpathBuilder.append("[");

                boolean firstOne = true;

                for (final Map.Entry<String, String> requiredAttribute : requiredAttributes.entrySet()) {

                    if (!firstOne) {
                        xpathBuilder.append(" and ");
                    }

                    xpathBuilder.append("@").append(requiredAttribute.getKey()).append(" = '")
                            .append(requiredAttribute.getValue()).append("'");

                    firstOne = false;
                }

                if (hasText) {
                    if (!firstOne) {
                        xpathBuilder.append(" and ");
                    }
                    xpathBuilder.append("text()='").append(text).append("'");
                }

                xpathBuilder.append("]");
            }

            logger.debug("returning by xpath string: " + xpathBuilder.toString());
        }

        @Override
        public List<WebElement> findElementsBy(final SearchContext searchContext) {
            final List<WebElement> matchingElems = super.findElementsBy(searchContext);

            if (matchingElems != null && matchingElems.size() < this.minimumExpected) {
                logger.info("expecting at least " + this.minimumExpected + " matching elems, found only "
                        + matchingElems.size() + " this time around");
                // we haven't found enough, clear out
                return null;
            }

            return matchingElems;
        }
    }

    static class ByTagAndAttributesWithValue extends ByTagAndAttributes {

        private final String value;

        ByTagAndAttributesWithValue(final String tagName, final Map<String, String> requiredAttributes,
                final String value) {
            super(tagName, requiredAttributes);
            this.value = value;

            // , value, 1);
        }

        ByTagAndAttributesWithValue(final String tagName, final Map<String, String> requiredAttributes,
                final String value, final int nth) {
            super(tagName, requiredAttributes, nth);

            this.value = value;
        }

        @Override
        public List<WebElement> findElementsBy(final SearchContext searchContext) {

            final List<WebElement> initialMatchingElems = super.findElementsBy(searchContext);

            List<WebElement> matchingElems = null;
            if (initialMatchingElems != null) {
                for (final WebElement e : initialMatchingElems) {
                    final String val = e.getAttribute("value");
                    if (val != null && val.compareTo(this.value) == 0) {

                        if (matchingElems == null) {
                            matchingElems = new ArrayList<WebElement>();
                        }
                        matchingElems.add(e);
                    }
                }
            }

            return matchingElems;
        }
    }



    public static ByTagWithCssClassWildcard ByTagWithCssClassWildcard(final String tagName,
                                                                      final String cssClassRegEx) {
        return new ByTagWithCssClassWildcard(tagName, cssClassRegEx, null);
    }

    public static ByTagWithCssClassWildcardAndTextMatching ByTagWithCssClassWildcardContainingText(final String tagName,
                                                                                                   final String cssClassRegEx, String text){
        return new ByTagWithCssClassWildcardAndTextMatching(tagName, cssClassRegEx, null, Matchers.containsString(text));
    }

    public static ByTagWithCssClassWildcard ByTagWithCssClassWildcard(final String tagName,
                                                                      final String cssClassRegEx,
                                                                      final String cssClassExcludesRegex) {
        return new ByTagWithCssClassWildcard(tagName, cssClassRegEx, cssClassExcludesRegex);
    }

    public static ByCssWithText ByCssWithText(final String cssClassName, final String expectedText) {
        return new ByCssWithText(cssClassName, expectedText);
    }

    public static ByCssContainingText ByCssContainingText(final String cssClassName, final String expectedText) {
        return new ByCssContainingText(cssClassName, expectedText);
    }


    public static ByCssSelectorWithText ByCssSelectorWithText(final String cssSelector, final String expectedText) {
        return new ByCssSelectorWithText(cssSelector, expectedText);
    }

    public static ByIdWithTextMatchingRegex ByIdWithTextMatchingRegex(final String id, final String regEx){
        return new ByIdWithTextMatchingRegex(id, regEx);
    }


    static class ByTagWithCssClassWildcard extends BaseBy {

        private final String tagName;
        private final Pattern cssClassRegEx;
        private final Pattern cssClassExcludesRegEx;

        private static Logger logger = LoggerFactory.getLogger(ByTagWithCssClassWildcard.class);

        ByTagWithCssClassWildcard(final String tagName, final String cssClassRegEx, final String cssClassExcludesRegEx) {
            this.tagName = tagName;
            this.cssClassRegEx = Pattern.compile(cssClassRegEx);

            if (cssClassExcludesRegEx != null){
                this.cssClassExcludesRegEx = Pattern.compile(cssClassExcludesRegEx);
            }
            else {
                this.cssClassExcludesRegEx = null;
            }
        }


        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            List<WebElement> matchingElems = null;
            boolean done = false;
            while (!done) {
                try {
                    final List<WebElement> tagElements = context.findElements(By.tagName(this.tagName));

                    for (final WebElement e : tagElements) {

                        String classString = e.getAttribute("class");

                        //System.out.println("got div class: " + classString);

                        if ((cssClassRegEx.matcher(classString).matches() && cssClassExcludesRegEx == null) ||
                                (cssClassRegEx.matcher(classString).matches() && cssClassExcludesRegEx != null && !cssClassExcludesRegEx.matcher(classString).matches())) {

                            if (matchingElems == null) {
                                matchingElems = new ArrayList<WebElement>();
                            }
                            matchingElems.add(e);
                        }
                    }
                    done = true;

                } catch (StaleElementReferenceException e) {
                    logger.debug("got a stale element exception");
                }
            }
            return matchingElems;
        }
    }


    static class ByTagWithCssClassWildcardAndTextMatching extends BaseBy {

        private final String tagName;
        private final Pattern cssClassRegEx;
        private final Pattern cssClassExcludesRegEx;
        private final Matcher<String> stringMatcher;


        ByTagWithCssClassWildcardAndTextMatching(final String tagName, final String cssClassRegEx,
                                                 final String cssClassExcludesRegEx, final Matcher<String> stringMatcher) {
            this.tagName = tagName;
            this.cssClassRegEx = Pattern.compile(cssClassRegEx);
            this.stringMatcher = stringMatcher;

            if (cssClassExcludesRegEx != null){
                this.cssClassExcludesRegEx = Pattern.compile(cssClassExcludesRegEx);
            }
            else {
                this.cssClassExcludesRegEx = null;
            }
        }


        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            List<WebElement> matchingElems = null;

            final List<WebElement> tagElements = context.findElements(By.tagName(this.tagName));

            for (final WebElement e : tagElements) {

                String classString = e.getAttribute("class");
                if (this.stringMatcher.matches(e.getText()) && ( (cssClassRegEx.matcher(classString).matches() && cssClassExcludesRegEx == null) ||
                        (cssClassRegEx.matcher(classString).matches() && cssClassExcludesRegEx != null && !cssClassExcludesRegEx.matcher(classString).matches()))){

                    if (matchingElems == null) {
                        matchingElems = new ArrayList<WebElement>();
                    }
                    matchingElems.add(e);
                }
            }

            return matchingElems;
        }
    }

    public static class ByIdWithTextMatchingRegex extends BaseBy {
        private static Logger logger = LoggerFactory.getLogger(ByIdWithTextMatchingRegex.class);
        protected final Pattern pattern;
        protected final String id;

        ByIdWithTextMatchingRegex(String id, String regEx) {
            this.id = id;
            this.pattern = Pattern.compile(regEx);
        }


        public List<WebElement> findElementsBy(SearchContext context) {
            List<WebElement> elems = context.findElements(By.id(this.id));

            if(elems != null) {
                if (elems.size() == 1){
                    String text = elems.get(0).getText();

                    if (pattern.matcher(text).matches()){
                        return elems;
                    }
                    else {
                        logger.debug("no reg ex match on text: " + text + " for regex: " + pattern.pattern());
                    }
                }
                else {
                    logger.error("To many elements found for Id: " + id);
                }
            }
            return null;
        }
    }


    static class ByCssWithText extends BaseBy {

        private final String cssClassName;
        private final String expectedText;


        ByCssWithText(final String cssClassName, final String expectedText) {
            this.cssClassName = cssClassName;
            this.expectedText = expectedText;
        }


        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            List<WebElement> matchingElems = null;

            final List<WebElement> tagElements = context.findElements(new ByClassName(this.cssClassName));

            for (final WebElement e : tagElements) {

                if (e.getText().equals(expectedText)){

                    if (matchingElems == null) {
                        matchingElems = new ArrayList<WebElement>();
                    }
                    matchingElems.add(e);
                }
            }

            return matchingElems;
        }
    }

    static class ByCssContainingText extends BaseBy {

        private final String cssClassName;
        private final String expectedText;


        ByCssContainingText(final String cssClassName, final String expectedText) {
            this.cssClassName = cssClassName;
            this.expectedText = expectedText;
        }


        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            List<WebElement> matchingElems = null;

            final List<WebElement> tagElements = context.findElements(new ByClassName(this.cssClassName));

            for (final WebElement e : tagElements) {

                if (e.getText().contains(expectedText)){

                    if (matchingElems == null) {
                        matchingElems = new ArrayList<WebElement>();
                    }
                    matchingElems.add(e);
                }
            }

            return matchingElems;
        }
    }

    static class ByCssSelectorWithText extends BaseBy {

        private final String cssSelector;
        private final String expectedText;


        ByCssSelectorWithText(final String cssSelector, final String expectedText) {
            this.cssSelector = cssSelector;
            this.expectedText = expectedText;
        }


        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            List<WebElement> matchingElems = null;

            final List<WebElement> tagElements = context.findElements(new ByCssSelector(this.cssSelector));

            for (final WebElement e : tagElements) {

                if (e.getText().equals(expectedText)){

                    if (matchingElems == null) {
                        matchingElems = new ArrayList<WebElement>();
                    }
                    matchingElems.add(e);
                }
            }

            return matchingElems;
        }
    }

}
