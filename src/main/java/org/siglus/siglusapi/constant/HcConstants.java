/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.constant;

import java.util.Set;
import org.javers.common.collections.Sets;

public class HcConstants {

  public static final String HCM_FACILITY_CODE = "01110101";
  public static final String HCB_FACILITY_CODE = "01070100";
  public static final String HCN_FACILITY_CODE = "01030100";
  public static final String HCQ_FACILITY_CODE = "01040125";
  private static final String COMMON_SERVICE_OUTROS = "Outros";

  // 51 services + 1
  public static final Set<String> HCM_SERVICES = Sets.asSet("Bloco operatorio central", "Bloco operatorio SUR",
      "Cirurgia I", "Cirurgia II", "Cirurgia III", "Cirurgia pediatrica", "Departamento de Cirurgia",
      "Departamento de Ginecologia", "Departamento de Medicina", "Departamento de Ortopedia",
      "Departamento de Pediatria", "Dose Unitaria", "Enfermaria de Oncologia", "Exames Especiais de Medicina",
      "Farmacia Ambulatorio Central", "Farmacia Ambulatorio SUR", "Gastroenterologia", "Hemodialise",
      "Laboratorio Central", "Laboratorio de Microbiologia", "Laboratorio de Pediatria", "Laboratorio de SUR",
      "Mal nutricao", "Medicina I", "Medicina II", "Medicina Legal", "Neurocirurgia", "Neurologia", "Ortopedia I",
      "Ortopedia II", "Ortopedia III", "Ortopedia IV", "Otorrinolaringologia", "Pediatria doencas gerais",
      "Pediatria Isolamento (infecto contagiosa)", "Pequenas Cirurgias SUR", "Preparacoes Galenicas", "Psiquiatria",
      "Radiologia", "Servico de Anatomia Patologica", "Servico de cardiologia", "Servico de Dermatologia",
      "Servico de Esterilizacao", "Servico de Estomatologia", "Servico de Fisioterapia", "Servico de Lavandaria",
      "Servico de Oncologia", "Servico de reanimacao-UCI", "Servicos HCM", "Traumatologia", "Unidade da DOR",
      COMMON_SERVICE_OUTROS);

  // 64 services + 1
  public static final Set<String> HCB_SERVICES = Sets.asSet("Bloco operatorio", "Departamento de Cirurgia (Cir 1 e 2)",
      "Departamento das Medicinas", "Departamento de Ortopedia", "Departamento de Pediatria",
      "Departamento das Pediatrias (UCIP, Latentes e Isolamentos)", "Farmacia geral (Publico)", "Farmacia de Urgencia",
      "Hemodialise", "Laboratorio de Urgencia", "Laboratorio de Referencia TB", "Laboratorio de Tuberculose",
      "Pedtria Malnutricao (URN)", "Medicina Legal", "Pequena Cirurgia", "Laboratorio de Preparacoes",
      "Servico de Psiquiatria", "Imagiologia (RX)", "Cardiologia", "Dermatologia", "Fisioterapia", "Lavandaria",
      "Servico de reanimacao/Banco de Socorros", "Centro de Triagem Covid 19", "SAP Farmacia", "SAP Internamento",
      "SAP Urgencia", "Secretaria Geral", "DEP NEP", "PCI", "Quarentena", "Bercario", "Banco de Sangue",
      "Consultas Externas", "Consulta de Trabalhadoes ", "Consulta de Ondoestomatologia",
      "Consulta externa de Oftalmologia", "Consulta externa de Ortopedia", "Consulta externa de Ginecologia",
      "Consulta externa de Otorrino", "Ex Hospital DIA (HDD)", "Centro Ortopedico ", "Departamento Hoteleiro",
      "Hematologia", "Traumatologia", "PAV", "Enfermarias", "Unidade de Cuidados Intensivos de Pediatria",
      "Departamento de Oftalmologia", "Laboratorio de Anatomia Patologica", "Morgue", "CERPIJ", "Incineradora",
      "Servico de Manutencao", "Farmacia das Enfermarias", "Medicina IV (Enf Modelo)", "Servicos de Urologia",
      "Oftalmologia Internamento", "Tisiologia", "Hospital de Dia Pediatrico", "SAAJ",
      "Servico de Urgencia de Pediatria (SUP)", "Servico de Obstetricia", "Sala de Partos", COMMON_SERVICE_OUTROS);

  // 50 services + 1
  public static final Set<String> HCQ_SERVICES = Sets.asSet("Bloco operatorio", "Cirurgia I", "Cirurgia II",
      "Ginecologia e Obstetricia", "Pediatria I", "Pediatria II", "Exames Especializados", "Farmacia geral",
      "Laboratorio", "Medicina I", "Medicina II", "Medicina Legal", "Ortopedia", "Otorrino", "Pequena Cirurgia",
      "Preparacao Galenica", "Imagiologia / Radiologia", "Anatomia Patologica", "Centro de Esterilizacao",
      "Estomatologia", "Fisioterapia", "Lavandaria", "SUR (SUG?)", "SICOV", "SAP", "Secretaria", "PCI", "Quarentena",
      "Banco de Sangue", "Consultas Externas", "Consulta do Trabalhador", "Ortoprotesia", "Oftalmologia",
      "Casa Mortoaria", "Manutencao", "Servico de Urgencia de Pediatria", "Direccao", "Servicos Gerais", "Neonatologia",
      "Accao Social", "Aprovisionamento", "Rouparia", "UCI", "Maternidade", "UTAO", "Clinica especial",
      "Quarentena de Psicotropico", "Sand Nutricao", "M.M. Cirurgico", "Gabinete de Covid19", COMMON_SERVICE_OUTROS);

  // +1
  public static final Set<String> HCN_SERVICES = Sets.asSet(COMMON_SERVICE_OUTROS);
}
