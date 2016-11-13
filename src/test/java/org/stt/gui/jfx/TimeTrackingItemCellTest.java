package org.stt.gui.jfx;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.gui.jfx.TimeTrackingItemCell.Builder;
import org.stt.gui.jfx.TimeTrackingItemCell.ContinueActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.DeleteActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.EditActionHandler;
import org.stt.model.TimeTrackingItem;
import org.stt.model.TimeTrackingItemFilter;
import org.stt.text.ItemGrouper;
import org.stt.text.WorktimeCategorizer;

import java.util.Arrays;
import java.util.ResourceBundle;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import org.stt.text.ItemCategorizer.ItemCategory;

public class TimeTrackingItemCellTest {

	private TimeTrackingItemCell sut;
	@Mock
	private ContinueActionHandler continueActionHandler;
	@Mock
	private Image imageForContinue;
	@Mock
	private EditActionHandler editActionHandler;
	@Mock
	private DeleteActionHandler deleteActionHandler;
	@Mock
	private Image imageForEdit;
	@Mock
	private Image imageForDelete;
	@Mock
	private Image imageFromTo;
	@Mock
	private Image runningImage;
	@Mock
	private TimeTrackingItemFilter firstItemOfTheDayFilter;
	
	@Mock
	private ItemGrouper itemGrouper;
	
	@Mock
	private WorktimeCategorizer workitemCategorizer;
	
	@Mock
	private TimeTrackingItemListConfig timeTrackingItemListConfig;

	@Before
	public void setup() throws Throwable {
		new JFXPanel();
		MockitoAnnotations.initMocks(this);
		
		given(timeTrackingItemListConfig.getBreakTimeColor()).willReturn("RED");
		given(timeTrackingItemListConfig.getGroupColors()).willReturn(Arrays.asList("BLUE"));
		
		given(workitemCategorizer.getCategory(anyString())).willReturn(ItemCategory.WORKTIME);
		
		Builder builder = new TimeTrackingItemCell.Builder();
		ResourceBundle resourceBundle = ResourceBundle
				.getBundle("org.stt.gui.Application");
		builder.continueActionHandler(continueActionHandler)
				.deleteActionHandler(deleteActionHandler)
				.editActionHandler(editActionHandler)
				.continueImage(imageForContinue).deleteImage(imageForDelete)
				.editImage(imageForEdit).runningImage(runningImage)
				.fromToImage(imageFromTo)
				.firstItemOfTheDayFilter(firstItemOfTheDayFilter)
				.resourceBundle(resourceBundle)
				.itemGrouper(itemGrouper)
				.workitemCategorizer(workitemCategorizer)
				.timeTrackingItemListConfig(timeTrackingItemListConfig);

		sut = new TimeTrackingItemCell(builder) {

			@Override
			protected void setupTooltips(ResourceBundle localization) {
			}
		};
	}

	@Test
	public void shouldUseContinueImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertPanelHasImageButtonWithImage(pane, imageForContinue);
	}

	@Test
	public void shouldUseFromToImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertPanelHasImageButtonWithImage(pane, imageForContinue);
	}

	@Test
	public void shouldUseEditImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertPanelHasImageButtonWithImage(pane, imageForEdit);
	}

	@Test
	public void shouldUseDeleteImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertPanelHasImageButtonWithImage(pane, imageForDelete);
	}

	private void assertPanelHasImageButtonWithImage(Pane pane, Image image) {
		assertThat(pane.getChildren(), hasItem(Matchers.<Node>hasProperty(
				"children",
				hasItem(Matchers.<Node>hasProperty("graphic",
								hasProperty("image", is(image)))))));
	}

	@Test
	public void shouldCallDeleteHandlerOnClickOnDelete() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());
		sut.updateItem(item, false);

		Button deleteButton = sut.deleteButton;

		// WHEN
		deleteButton.fire();

		// THEN
		verify(deleteActionHandler).delete(item);
	}

	@Test
	public void shouldCallContinueHandlerOnClickOnContinue() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());
		sut.updateItem(item, false);

		Button continueButton = sut.continueButton;

		// WHEN
		continueButton.fire();

		// THEN
		verify(continueActionHandler).continueItem(item);
	}

	@Test
	public void shouldCallEditHandlerOnClickOnEdit() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());
		sut.updateItem(item, false);

		Button editButton = sut.editButton;
		// WHEN
		editButton.fire();

		// THEN
		verify(editActionHandler).edit(item);
	}
}
