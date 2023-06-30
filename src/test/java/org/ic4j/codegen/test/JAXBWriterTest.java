package org.ic4j.codegen.test;


import org.ic4j.candid.jaxb.JAXBUtils;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.codegen.MotokoWriter;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prowidesoftware.swift.model.mx.AppHdr;

import com.prowidesoftware.swift.model.mx.dic.FIToFICustomerCreditTransferV10;
import com.prowidesoftware.swift.model.mx.dic.FinancialInstitutionCreditTransferV10;
import com.prowidesoftware.swift.model.mx.dic.PaymentReturnV11;
import com.prowidesoftware.swift.model.mx.dic.RemittanceInformation20;
import com.prowidesoftware.swift.model.mx.dic.FIToFIPaymentStatusReportV12;
import com.prowidesoftware.swift.model.mx.dic.FIToFICustomerDirectDebitV09;
import com.prowidesoftware.swift.model.mx.dic.FIToFIPaymentReversalV11;
import com.prowidesoftware.swift.model.mx.dic.FinancialInstitutionDirectDebitV05;
import com.prowidesoftware.swift.model.mx.dic.FIToFIPaymentStatusRequestV05;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader70;
import com.prowidesoftware.swift.model.mx.dic.TaxRecordPeriod1Code;
import com.prowidesoftware.swift.model.mx.MxPain00100103;
import com.prowidesoftware.swift.model.mx.dic.CustomerCreditTransferInitiationV11;
import com.prowidesoftware.swift.model.mx.dic.CustomerPaymentStatusReportV12;
import com.prowidesoftware.swift.model.mx.dic.CustomerCreditTransferInitiationV03;
import com.prowidesoftware.swift.model.mx.dic.CustomerPaymentStatusReportV03;
import com.prowidesoftware.swift.model.mx.dic.CustomerPaymentReversalV11;
import com.prowidesoftware.swift.model.mx.dic.CustomerDirectDebitInitiationV10;
import com.prowidesoftware.swift.model.mx.dic.MandateInitiationRequestV07;
import com.prowidesoftware.swift.model.mx.dic.MandateAmendmentRequestV07;
import com.prowidesoftware.swift.model.mx.dic.MandateCancellationRequestV07;
import com.prowidesoftware.swift.model.mx.dic.MandateAcceptanceReportV07;
import com.prowidesoftware.swift.model.mx.dic.MandateCopyRequestV03;
import com.prowidesoftware.swift.model.mx.dic.MandateSuspensionRequestV03;
import com.prowidesoftware.swift.model.mx.dic.CreditorPaymentActivationRequestV09;
import com.prowidesoftware.swift.model.mx.dic.CreditorPaymentActivationRequestStatusReportV09;

import io.dscope.rosettanet.domain.procurement.procurement.v02_29.OrderShippingInformation;
import io.dscope.rosettanet.interchange.purchaseorderrequest.v02_05.PurchaseOrderRequestType;

public class JAXBWriterTest {
	static Logger LOG;

	static final String MOTOKO_FIToFICustomerCreditTransferV10_TYPES_FILE = "FIToFICustomerCreditTransferV10Types.mo";
	static final String MOTOKO_FinancialInstitutionCreditTransferV10_TYPES_FILE = "FinancialInstitutionCreditTransferV10Types.mo";
	static final String MOTOKO_PaymentReturnV11_TYPES_FILE = "PaymentReturnV11Types.mo";
	static final String MOTOKO_FIToFIPaymentStatusReportV12_TYPES_FILE = "FIToFIPaymentStatusReportV12Types.mo";
	static final String MOTOKO_FIToFICustomerDirectDebitV09_TYPES_FILE = "FIToFICustomerDirectDebitV09Types.mo";
	static final String MOTOKO_FIToFIPaymentReversalV11_TYPES_FILE = "FIToFIPaymentReversalV11Types.mo";
	static final String MOTOKO_FinancialInstitutionDirectDebitV05_TYPES_FILE = "FinancialInstitutionDirectDebitV05Types.mo";
	static final String MOTOKO_FIToFIPaymentStatusRequestV05_TYPES_FILE = "FIToFIPaymentStatusRequestV05Types.mo";
	
	static final String MOTOKO_MxPain00100103_TYPES_FILE = "MxPain00100103Types.mo";
	static final String MOTOKO_CustomerCreditTransferInitiationV11_TYPES_FILE = "CustomerCreditTransferInitiationV11Types.mo";
	static final String MOTOKO_CustomerPaymentStatusReportV12_TYPES_FILE = "CustomerPaymentStatusReportV12Types.mo";
	static final String MOTOKO_CustomerCreditTransferInitiationV03_TYPES_FILE = "CustomerCreditTransferInitiationV03Types.mo";
	static final String MOTOKO_CustomerPaymentStatusReportV03_TYPES_FILE = "CustomerPaymentStatusReportV03Types.mo";
	static final String MOTOKO_CustomerPaymentReversalV11_TYPES_FILE = "CustomerPaymentReversalV11Types.mo";
	static final String MOTOKO_CustomerDirectDebitInitiationV10_TYPES_FILE = "CustomerDirectDebitInitiationV10Types.mo";
	static final String MOTOKO_MandateInitiationRequestV07_TYPES_FILE = "MandateInitiationRequestV07Types.mo";
	static final String MOTOKO_MandateAmendmentRequestV07_TYPES_FILE = "MandateAmendmentRequestV07Types.mo";
	static final String MOTOKO_MandateCancellationRequestV07_TYPES_FILE = "MandateCancellationRequestV07Types.mo";
	static final String MOTOKO_MandateAcceptanceReportV07_TYPES_FILE = "MandateAcceptanceReportV07Types.mo";
	static final String MOTOKO_MandateCopyRequestV03_TYPES_FILE = "MandateCopyRequestV03Types.mo";
	static final String MOTOKO_MandateSuspensionRequestV03_TYPES_FILE = "MandateSuspensionRequestV03Types.mo";
	static final String MOTOKO_CreditorPaymentActivationRequestV09_TYPES_FILE = "CreditorPaymentActivationRequestV09Types.mo";
	static final String MOTOKO_CreditorPaymentActivationRequestStatusReportV09_TYPES_FILE = "CreditorPaymentActivationRequestStatusReportV09Types.mo";	
	
	static final String MOTOKO_PurchaseOrderRequestV02_05_TYPES_FILE = "PurchaseOrderRequestV02_05Types.mo";	

	static {
		LOG = LoggerFactory.getLogger(JAXBWriterTest.class);
	}

	@Test
	public void test() {

		try {
			IDLType type = JAXBUtils.getIDLType(OrderShippingInformation.class);
			
			type = JAXBUtils.getIDLType(RemittanceInformation20.class);
			
			type = JAXBUtils.getIDLType(GroupHeader70.class);					
			
			type = JAXBUtils.getIDLType(TaxRecordPeriod1Code.class);
				
			type = JAXBUtils.getIDLType(AppHdr.class);
			
			MotokoWriter motokoWriter = new MotokoWriter();
			
			motokoWriter.nameConstructor.convertName = true;
			motokoWriter.nameConstructor.hasPostfix = true;
			motokoWriter.nameConstructor.postfixId = 1;
			
			String outDir = "test/RosettaNet/Motoko/pip3a4";
			
			if(Files.notExists(Paths.get(outDir)))
				Files.createDirectories(Paths.get(outDir));
			
			type = JAXBUtils.getIDLType(PurchaseOrderRequestType.class);
			motokoWriter.writeFile(Paths.get(outDir,MOTOKO_PurchaseOrderRequestV02_05_TYPES_FILE), type);
			
			motokoWriter.nameConstructor.convertName = false;
			motokoWriter.nameConstructor.hasPostfix = false;
			
			outDir = "test/ISO20022/Motoko/pacs";
			
			if(Files.notExists(Paths.get(outDir)))
				Files.createDirectories(Paths.get(outDir));
			
			type = JAXBUtils.getIDLType(FIToFICustomerCreditTransferV10.class);
			motokoWriter.writeFile(Paths.get(outDir,MOTOKO_FIToFICustomerCreditTransferV10_TYPES_FILE), type);			
			
			type = JAXBUtils.getIDLType(FinancialInstitutionCreditTransferV10.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_FinancialInstitutionCreditTransferV10_TYPES_FILE), type);		
			
			type = JAXBUtils.getIDLType(PaymentReturnV11.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_PaymentReturnV11_TYPES_FILE), type);	
			
			type = JAXBUtils.getIDLType(FIToFIPaymentStatusReportV12.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_FIToFIPaymentStatusReportV12_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(FIToFICustomerDirectDebitV09.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_FIToFICustomerDirectDebitV09_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(FIToFIPaymentReversalV11.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_FIToFIPaymentReversalV11_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(FinancialInstitutionDirectDebitV05.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_FinancialInstitutionDirectDebitV05_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(FIToFIPaymentStatusRequestV05.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_FIToFIPaymentStatusRequestV05_TYPES_FILE), type);
			
			outDir = "test/ISO20022/Motoko/pain";
			
			if(Files.notExists(Paths.get(outDir)))
				Files.createDirectories(Paths.get(outDir));
			
			type = JAXBUtils.getIDLType(MxPain00100103.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_MxPain00100103_TYPES_FILE), type);

			
			type = JAXBUtils.getIDLType(CustomerCreditTransferInitiationV03.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_CustomerCreditTransferInitiationV03_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(CustomerPaymentStatusReportV03.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_CustomerPaymentStatusReportV03_TYPES_FILE), type);

			
			type = JAXBUtils.getIDLType(CustomerCreditTransferInitiationV11.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_CustomerCreditTransferInitiationV11_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(CustomerPaymentStatusReportV12.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_CustomerPaymentStatusReportV12_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(CustomerPaymentReversalV11.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_CustomerPaymentReversalV11_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(CustomerDirectDebitInitiationV10.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_CustomerDirectDebitInitiationV10_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(MandateInitiationRequestV07.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_MandateInitiationRequestV07_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(MandateAmendmentRequestV07.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_MandateAmendmentRequestV07_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(MandateCancellationRequestV07.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_MandateCancellationRequestV07_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(MandateAcceptanceReportV07.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_MandateAcceptanceReportV07_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(MandateCopyRequestV03.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_MandateCopyRequestV03_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(MandateSuspensionRequestV03.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_MandateSuspensionRequestV03_TYPES_FILE), type);	
			
			type = JAXBUtils.getIDLType(CreditorPaymentActivationRequestV09.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_CreditorPaymentActivationRequestV09_TYPES_FILE), type);
			
			type = JAXBUtils.getIDLType(CreditorPaymentActivationRequestStatusReportV09.class);
			
			motokoWriter.writeFile(Paths.get(outDir, MOTOKO_CreditorPaymentActivationRequestStatusReportV09_TYPES_FILE), type);				
						
			
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
		}

	}
}
