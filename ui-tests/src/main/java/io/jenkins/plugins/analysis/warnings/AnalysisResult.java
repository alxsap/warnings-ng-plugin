package io.jenkins.plugins.analysis.warnings;

import java.net.URL;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.google.inject.Injector;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

import io.jenkins.plugins.analysis.warnings.IssuesDetailsTable.IssuesTableRowType;

/**
 * {@link PageObject} representing the details page of the static analysis tool results.
 *
 * @author Stephan Plöderl
 * @author Ullrich Hafner
 */
public class AnalysisResult extends PageObject {
    private static final String[] DRY_TOOLS = {"cpd", "simian", "dupfinder"};

    private final String id;

    /**
     * Creates an instance of the page displaying the details of the issues for a specific tool.
     *
     * @param parent
     *         a finished build configured with a static analysis tool
     * @param id
     *         the type of the result page (e.g. simian, checkstyle, cpd, etc.)
     */
    public AnalysisResult(final Build parent, final String id) {
        super(parent, parent.url(id));

        this.id = id;
    }

    /**
     * Creates an instance of the page displaying the details of the issues. This constructor is used for injecting a
     * filtered instance of the page (e.g. by clicking on links which open a filtered instance of a AnalysisResult.
     *
     * @param injector
     *         the injector of the page
     * @param url
     *         the url of the page
     * @param id
     *         the id of  the result page (e.g simian or cpd)
     */
    @SuppressWarnings("unused") // Required to dynamically create page object using reflection
    public AnalysisResult(final Injector injector, final URL url, final String id) {
        super(injector, url);

        this.id = id;
    }

    /**
     * Returns the active and visible tab that has the focus in the tab bar.
     *
     * @return the active tab
     */
    public Tab getActiveTab() {
        WebElement activeTab = find(By.xpath("//a[@role='tab' and contains(@class, 'active')]"));

        return Tab.valueWithHref(extractRelativeUrl(activeTab.getAttribute("href")));
    }

    /**
     * Returns the list of available tabs. These tabs depend on the available properites of the set of shown issues.
     *
     * @return the available tabs
     */
    public Collection<Tab> getAvailableTabs() {
        return all(By.xpath("//a[@role='tab']")).stream()
                .map(tab -> tab.getAttribute("href"))
                .map(this::extractRelativeUrl)
                .map(Tab::valueWithHref)
                .collect(Collectors.toList());
    }

    private String extractRelativeUrl(final String absoluteUrl) {
        return "#" + StringUtils.substringAfterLast(absoluteUrl, "#");
    }

    /**
     * Returns the total number of issues. This method requires that one of the tabs is shown that shows the total
     * number of issues in the footer. I.e. the {@link Tab#ISSUES} and {@link Tab#BLAMES}.
     *
     * @return the total number of issues
     */
    public int getTotal() {
        String total = find(By.tagName("tfoot")).getText();

        return Integer.parseInt(StringUtils.substringAfter(total, "Total "));
    }

    /**
     * Returns the type of the rows in the issues table. Currently, code duplications have a different representation
     * than all other static analysis tools.
     *
     * @return the row type
     */
    private IssuesTableRowType getIssuesTableType() {
        if (ArrayUtils.contains(DRY_TOOLS, id)) {
            return IssuesTableRowType.DRY;
        }
        return IssuesTableRowType.DEFAULT;
    }

    /**
     * Reloads the {@link PageObject}.
     */
    public void reload() {
        open();
    }

    /**
     * Opens the analysis details page and selects the specified tab.
     *
     * @param tab
     *         the tab that should be selected
     */
    public void openTab(final Tab tab) {
        open();

        WebElement tabElement = getElement(By.id("tab-details")).findElement(tab.getXpath());
        tabElement.click();
    }

    /**
     * Opens the analysis details page, selects the tab {@link Tab#ISSUES} and returns the {@link PageObject} of the
     * issues table.
     *
     * @return page object of the issues table.
     */
    public IssuesDetailsTable openIssuesTable() {
        openTab(Tab.ISSUES);

        WebElement issuesTab = find(By.id("issuesContent"));
        return new IssuesDetailsTable(issuesTab, this, getIssuesTableType());
    }

    /**
     * Opens the analysis details page, selects the tab {@link Tab#CATEGORIES} and returns the {@link PageObject} of the
     * categories table.
     *
     * @param tab
     *         the tab to open
     *
     * @return page object of the categories table.
     */
    public PropertyDetailsTable openPropertiesTable(final Tab tab) {
        openTab(tab);

        WebElement table = find(By.id(tab.contentId));
        return new PropertyDetailsTable(table, this, tab.property);
    }

    /**
     * Opens a link on the page leading to another page.
     *
     * @param element
     *         the WebElement representing the link to be clicked
     * @param type
     *         the class of the PageObject which represents the page to which the link leads to
     * @param <T>
     *         actual type of the page object
     *
     * @return the instance of the PageObject to which the link leads to
     */
    // FIXME: IssuesTable should not depend on AnalysisResult
    public <T extends PageObject> T openLinkOnSite(final WebElement element, final Class<T> type) {
        String link = element.getAttribute("href");
        T retVal = newInstance(type, injector, url(link));
        element.click();
        return retVal;
    }

    /**
     * Method for getting the row length select element by the currently active tab.
     *
     * @return Select WebElement where the user can choose how many rows should be displayed.
     */
    public Select getLengthSelectElementByActiveTab() {
        WebElement lengthSelect = find(By.id(this.getActiveTab().property + "_length"));
        return new Select(lengthSelect.findElement(By.cssSelector("label > select")));
    }

    /**
     * Method for getting the paginate WebElement for any active tab.
     *
     * @return parent WebElement that contains the paginate buttons for a result table.
     */
    public WebElement getInfoElementByActiveTab() {
        return this.getElement(By.id(this.getActiveTab().property + "_info"));
    }

    /**
     * Method for getting the paginate WebElement for any active tab.
     *
     * @return parent WebElement that contains the paginate buttons for a result table.
     */
    public WebElement getPaginateElementByActiveTab() {
        return this.getElement(By.id(this.getActiveTab().property + "_paginate"));
    }

    /**
     * Method for getting the input field of any active tab.
     *
     * @return WebElement where a user can filter the table by text input.
     */
    public WebElement getFilterInputElementByActiveTab() {
        WebElement filter = find(By.id(this.getActiveTab().property + "_filter"));
        return filter.findElement(By.cssSelector("label > input"));
    }

    /**
     * Opens a link to a filtered version of this AnalysisResult by clicking on a link.
     *
     * @param element
     *         the WebElement representing the link to be clicked
     *
     * @return the instance of the filtered AnalysisResult
     */
    // FIXME: IssuesTable should not depend on AnalysisResult
    public AnalysisResult openFilterLinkOnSite(final WebElement element) {
        String link = element.getAttribute("href");
        AnalysisResult retVal = newInstance(AnalysisResult.class, injector, url(link), id);
        element.click();
        return retVal;
    }

    /**
     * Enum representing the possible tabs which can be opened in the {@link AnalysisResult} details view.
     */
    public enum Tab {
        TOOLS("origin"),
        MODULES("moduleName"),
        PACKAGES("packageName"),
        FOLDERS("folder"),
        FILES("fileName"),
        CATEGORIES("category"),
        TYPES("type"),
        ISSUES("issues"),
        BLAMES("scm");

        private final String contentId;
        private final String property;

        Tab(final String property) {
            this.property = property;
            contentId = property + "Content";
        }

        /**
         * Returns the selenium {@link By} selector to find the specific tab.
         *
         * @return the selenium filter rule
         */
        By getXpath() {
            return By.xpath("//a[@href='#" + contentId + "']");
        }

        /**
         * Returns the enum element that has the specified href property.
         *
         * @param href
         *         the href to select the tab
         *
         * @return the tab
         * @throws NoSuchElementException
         *         if the tab could not be found
         */
        static Tab valueWithHref(final String href) {
            for (Tab tab : Tab.values()) {
                if (tab.contentId.equals(href.substring(1))) {
                    return tab;
                }
            }
            throw new NoSuchElementException("No such tab with href " + href);
        }
    }
}
