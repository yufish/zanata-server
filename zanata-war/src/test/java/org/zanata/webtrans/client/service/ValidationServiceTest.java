package org.zanata.webtrans.client.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.customware.gwt.presenter.client.EventBus;

import org.hamcrest.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.webtrans.client.events.DocumentSelectionEvent;
import org.zanata.webtrans.client.events.RequestValidationEvent;
import org.zanata.webtrans.client.events.RunValidationEvent;
import org.zanata.webtrans.client.events.TransUnitSelectionEvent;
import org.zanata.webtrans.client.presenter.UserConfigHolder;
import org.zanata.webtrans.client.resources.TableEditorMessages;
import org.zanata.webtrans.client.resources.ValidationMessages;
import org.zanata.webtrans.client.rpc.CachingDispatchAsync;
import org.zanata.webtrans.client.ui.HasUpdateValidationWarning;
import org.zanata.webtrans.server.locale.Gwti18nReader;
import org.zanata.webtrans.shared.model.ValidationAction;
import org.zanata.webtrans.shared.model.ValidationId;
import org.zanata.webtrans.shared.model.ValidationInfo;
import org.zanata.webtrans.shared.validation.ValidationFactory;

import com.google.common.collect.Lists;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Test(groups = "unit-tests")
public class ValidationServiceTest
{
   public static final ValidationId VAL_KEY = ValidationId.HTML_XML;
   private ValidationService service;
   @Mock
   private EventBus eventBus;
   @Mock
   private TableEditorMessages messages;
   
   private ValidationMessages validationMessages;
   @Mock
   private HasUpdateValidationWarning validationMessagePanel;
   
   @Mock
   private CachingDispatchAsync dispatcher;
   
   @Mock
   private UserConfigHolder configHolder;

   @BeforeMethod
   public void beforeMethod() throws IOException
   {
      MockitoAnnotations.initMocks(this);

      validationMessages = Gwti18nReader.create(ValidationMessages.class);

      service = new ValidationService(eventBus, messages, validationMessages, configHolder);
      ValidationFactory validationFactory = new ValidationFactory(validationMessages);

      Map<ValidationId, ValidationAction> validationMap = validationFactory.getAllValidationActions();
      ArrayList<ValidationInfo> validationInfoList = new ArrayList<ValidationInfo>();
      for (ValidationAction action : validationMap.values())
      {
         action.getValidationInfo().setEnabled(true);
         validationInfoList.add(action.getValidationInfo());
      }
      service.setValidationRules(validationInfoList);

      when(messages.notifyValidationError()).thenReturn("validation error");
      verify(eventBus).addHandler(RunValidationEvent.getType(), service);
      verify(eventBus).addHandler(TransUnitSelectionEvent.getType(), service);
      verify(eventBus).addHandler(DocumentSelectionEvent.getType(), service);
   }

   @Test
   public void onValidate()
   {
      RunValidationEvent event = new RunValidationEvent("source", "target %s", false);
      event.addWidget(validationMessagePanel);
      ArrayList<String> errors = Lists.newArrayList(validationMessages.varsAdded(Arrays.asList("%s")), validationMessages.varsAdded(Arrays.asList("%s")));

      service.onValidate(event);

      verify(validationMessagePanel).updateValidationWarning(errors);
   }

   @Test
   public void onTransUnitSelectionWillClearMessages()
   {
      ValidationService validationServiceSpy = Mockito.spy(service);
      doNothing().when(validationServiceSpy).clearAllMessage();

      validationServiceSpy.onTransUnitSelected(null);

      verify(validationServiceSpy).clearAllMessage();
   }

   @Test
   public void onDocumentSelectionWillClearMessages()
   {
      ValidationService validationServiceSpy = Mockito.spy(service);
      doNothing().when(validationServiceSpy).clearAllMessage();

      validationServiceSpy.onDocumentSelected(null);

      verify(validationServiceSpy).clearAllMessage();
   }

   @Test
   public void canClearAllErrorMessages()
   {
      service.clearAllMessage();

      List<ValidationAction> validationList = new ArrayList<ValidationAction>(service.getValidationMap().values());

      for (ValidationAction action : validationList)
      {
         assertThat(action.getError().size(), Matchers.equalTo(0));
      }
   }

   @Test
   public void canUpdateValidatorStatus()
   {
      service.updateStatus(VAL_KEY, false);

      ValidationAction validationAction = service.getValidationMap().get(VAL_KEY);

      assertThat(validationAction.getValidationInfo().isEnabled(), Matchers.equalTo(false));
      verify(eventBus).fireEvent(RequestValidationEvent.EVENT);
   }

   @Test
   public void canGetValidationList()
   {
      List<ValidationAction> validationList = new ArrayList<ValidationAction>(service.getValidationMap().values());
      
      assertThat(validationList.size(), Matchers.equalTo(7));
   }

}
