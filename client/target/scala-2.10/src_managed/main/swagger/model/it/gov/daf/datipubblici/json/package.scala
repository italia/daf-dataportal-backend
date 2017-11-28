package it.gov.daf.datipubblici

import play.api.libs.json._
import play.api.libs.functional.syntax._

package object json {
  implicit lazy val DatasetTestReads: Reads[DatasetTest] = Reads[DatasetTest] {
    json => JsSuccess(DatasetTest((json \ "test_multi").asOpt[String], (json \ "groups").asOpt[List[Group]]))
  }
  implicit lazy val DatasetTestWrites: Writes[DatasetTest] = Writes[DatasetTest] {
    o => JsObject(Seq("test_multi" -> Json.toJson(o.test_multi), "groups" -> Json.toJson(o.groups)).filter(_._2 != JsNull))
  }
  implicit lazy val CatalogReads: Reads[Catalog] = Reads[Catalog] {
    json => JsSuccess(Catalog((json \ "name").asOpt[String]))
  }
  implicit lazy val CatalogWrites: Writes[Catalog] = Writes[Catalog] {
    o => JsObject(Seq("name" -> Json.toJson(o.name)).filter(_._2 != JsNull))
  }
  implicit lazy val DistributionReads: Reads[Distribution] = Reads[Distribution] {
    json => JsSuccess(Distribution((json \ "label").asOpt[String], (json \ "count").asOpt[Float]))
  }
  implicit lazy val DistributionWrites: Writes[Distribution] = Writes[Distribution] {
    o => JsObject(Seq("label" -> Json.toJson(o.label), "count" -> Json.toJson(o.count)).filter(_._2 != JsNull))
  }
  implicit lazy val BrokenLinkReads: Reads[BrokenLink] = Reads[BrokenLink] {
    json => JsSuccess(BrokenLink((json \ "label").asOpt[String], (json \ "url").asOpt[String], (json \ "m_status").asOpt[String], (json \ "name").asOpt[String], (json \ "rurl").asOpt[String], (json \ "dataset_url").asOpt[String], (json \ "catalog_name").asOpt[String]))
  }
  implicit lazy val BrokenLinkWrites: Writes[BrokenLink] = Writes[BrokenLink] {
    o => JsObject(Seq("label" -> Json.toJson(o.label), "url" -> Json.toJson(o.url), "m_status" -> Json.toJson(o.m_status), "name" -> Json.toJson(o.name), "rurl" -> Json.toJson(o.rurl), "dataset_url" -> Json.toJson(o.dataset_url), "catalog_name" -> Json.toJson(o.catalog_name)).filter(_._2 != JsNull))
  }
  implicit lazy val DatasetReads: Reads[Dataset] = Reads[Dataset] {
    json => JsSuccess(Dataset((json \ "alternate_identifier").asOpt[String], (json \ "author").asOpt[String], (json \ "author_email").asOpt[String], (json \ "frequency").asOpt[String], (json \ "groups").asOpt[List[Group]], (json \ "holder_identifier").asOpt[String], (json \ "holder_name").asOpt[String], (json \ "identifier").asOpt[String], (json \ "license_id").asOpt[String], (json \ "license_title").asOpt[String], (json \ "modified").asOpt[String], (json \ "name").asOpt[String], (json \ "notes").asOpt[String], (json \ "organization").asOpt[Organization], (json \ "owner_org").asOpt[String], (json \ "publisher_identifier").asOpt[String], (json \ "publisher_name").asOpt[String], (json \ "relationships_as_object").asOpt[List[Relationship]], (json \ "relationships_as_subject").asOpt[List[Relationship]], (json \ "resources").asOpt[List[Resource]], (json \ "tags").asOpt[List[Tag]], (json \ "theme").asOpt[String]))
  }
  implicit lazy val DatasetWrites: Writes[Dataset] = Writes[Dataset] {
    o => JsObject(Seq("alternate_identifier" -> Json.toJson(o.alternate_identifier), "author" -> Json.toJson(o.author), "author_email" -> Json.toJson(o.author_email), "frequency" -> Json.toJson(o.frequency), "groups" -> Json.toJson(o.groups), "holder_identifier" -> Json.toJson(o.holder_identifier), "holder_name" -> Json.toJson(o.holder_name), "identifier" -> Json.toJson(o.identifier), "license_id" -> Json.toJson(o.license_id), "license_title" -> Json.toJson(o.license_title), "modified" -> Json.toJson(o.modified), "name" -> Json.toJson(o.name), "notes" -> Json.toJson(o.notes), "organization" -> Json.toJson(o.organization), "owner_org" -> Json.toJson(o.owner_org), "publisher_identifier" -> Json.toJson(o.publisher_identifier), "publisher_name" -> Json.toJson(o.publisher_name), "relationships_as_object" -> Json.toJson(o.relationships_as_object), "relationships_as_subject" -> Json.toJson(o.relationships_as_subject), "resources" -> Json.toJson(o.resources), "tags" -> Json.toJson(o.tags), "theme" -> Json.toJson(o.theme)).filter(_._2 != JsNull))
  }
  implicit lazy val GroupReads: Reads[Group] = Reads[Group] {
    json => JsSuccess(Group((json \ "display_name").asOpt[String], (json \ "description").asOpt[String], (json \ "image_display_url").asOpt[String], (json \ "title").asOpt[String], (json \ "id").asOpt[String], (json \ "name").asOpt[String]))
  }
  implicit lazy val GroupWrites: Writes[Group] = Writes[Group] {
    o => JsObject(Seq("display_name" -> Json.toJson(o.display_name), "description" -> Json.toJson(o.description), "image_display_url" -> Json.toJson(o.image_display_url), "title" -> Json.toJson(o.title), "id" -> Json.toJson(o.id), "name" -> Json.toJson(o.name)).filter(_._2 != JsNull))
  }
  implicit lazy val OrganizationReads: Reads[Organization] = Reads[Organization] {
    json => JsSuccess(Organization((json \ "approval_status").asOpt[String], (json \ "created").asOpt[String], (json \ "description").asOpt[String], (json \ "email").asOpt[String], (json \ "id").asOpt[String], (json \ "image_url").asOpt[String], (json \ "is_organization").asOpt[Boolean], (json \ "name").asOpt[String], (json \ "revision_id").asOpt[String], (json \ "state").asOpt[String], (json \ "title").asOpt[String], (json \ "type").asOpt[String], (json \ "users").asOpt[List[UserOrg]]))
  }
  implicit lazy val OrganizationWrites: Writes[Organization] = Writes[Organization] {
    o => JsObject(Seq("approval_status" -> Json.toJson(o.approval_status), "created" -> Json.toJson(o.created), "description" -> Json.toJson(o.description), "email" -> Json.toJson(o.email), "id" -> Json.toJson(o.id), "image_url" -> Json.toJson(o.image_url), "is_organization" -> Json.toJson(o.is_organization), "name" -> Json.toJson(o.name), "revision_id" -> Json.toJson(o.revision_id), "state" -> Json.toJson(o.state), "title" -> Json.toJson(o.title), "type" -> Json.toJson(o.`type`), "users" -> Json.toJson(o.users)).filter(_._2 != JsNull))
  }
  implicit lazy val RelationshipReads: Reads[Relationship] = Reads[Relationship] {
    json => JsSuccess(Relationship((json \ "subject").asOpt[String], (json \ "object").asOpt[String], (json \ "type").asOpt[String], (json \ "comment").asOpt[String]))
  }
  implicit lazy val RelationshipWrites: Writes[Relationship] = Writes[Relationship] {
    o => JsObject(Seq("subject" -> Json.toJson(o.subject), "object" -> Json.toJson(o.`object`), "type" -> Json.toJson(o.`type`), "comment" -> Json.toJson(o.comment)).filter(_._2 != JsNull))
  }
  implicit lazy val ResourceReads: Reads[Resource] = Reads[Resource] {
    json => JsSuccess(Resource((json \ "cache_last_updated").asOpt[String], (json \ "cache_url").asOpt[String], (json \ "created").asOpt[String], (json \ "datastore_active").asOpt[Boolean], (json \ "description").asOpt[String], (json \ "distribution_format").asOpt[String], (json \ "format").asOpt[String], (json \ "hash").asOpt[String], (json \ "id").asOpt[String], (json \ "last_modified").asOpt[String], (json \ "mimetype").asOpt[String], (json \ "mimetype_inner").asOpt[String], (json \ "name").asOpt[String], (json \ "package_id").asOpt[String], (json \ "position").asOpt[Int], (json \ "resource_type").asOpt[String], (json \ "revision_id").asOpt[String], (json \ "size").asOpt[Int], (json \ "state").asOpt[String], (json \ "url").asOpt[String]))
  }
  implicit lazy val ResourceWrites: Writes[Resource] = Writes[Resource] {
    o => JsObject(Seq("cache_last_updated" -> Json.toJson(o.cache_last_updated), "cache_url" -> Json.toJson(o.cache_url), "created" -> Json.toJson(o.created), "datastore_active" -> Json.toJson(o.datastore_active), "description" -> Json.toJson(o.description), "distribution_format" -> Json.toJson(o.distribution_format), "format" -> Json.toJson(o.format), "hash" -> Json.toJson(o.hash), "id" -> Json.toJson(o.id), "last_modified" -> Json.toJson(o.last_modified), "mimetype" -> Json.toJson(o.mimetype), "mimetype_inner" -> Json.toJson(o.mimetype_inner), "name" -> Json.toJson(o.name), "package_id" -> Json.toJson(o.package_id), "position" -> Json.toJson(o.position), "resource_type" -> Json.toJson(o.resource_type), "revision_id" -> Json.toJson(o.revision_id), "size" -> Json.toJson(o.size), "state" -> Json.toJson(o.state), "url" -> Json.toJson(o.url)).filter(_._2 != JsNull))
  }
  implicit lazy val TagReads: Reads[Tag] = Reads[Tag] {
    json => JsSuccess(Tag((json \ "display_name").asOpt[String], (json \ "id").asOpt[String], (json \ "name").asOpt[String], (json \ "state").asOpt[String], (json \ "vocabulary_id").asOpt[String]))
  }
  implicit lazy val TagWrites: Writes[Tag] = Writes[Tag] {
    o => JsObject(Seq("display_name" -> Json.toJson(o.display_name), "id" -> Json.toJson(o.id), "name" -> Json.toJson(o.name), "state" -> Json.toJson(o.state), "vocabulary_id" -> Json.toJson(o.vocabulary_id)).filter(_._2 != JsNull))
  }
  implicit lazy val UserReads: Reads[User] = Reads[User] {
    json => JsSuccess(User((json \ "name").asOpt[String], (json \ "email").asOpt[String], (json \ "password").asOpt[String], (json \ "fullname").asOpt[String], (json \ "about").asOpt[String]))
  }
  implicit lazy val UserWrites: Writes[User] = Writes[User] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "email" -> Json.toJson(o.email), "password" -> Json.toJson(o.password), "fullname" -> Json.toJson(o.fullname), "about" -> Json.toJson(o.about)).filter(_._2 != JsNull))
  }
  implicit lazy val UserOrgReads: Reads[UserOrg] = Reads[UserOrg] {
    json => JsSuccess(UserOrg((json \ "name").asOpt[String], (json \ "capacity").asOpt[String]))
  }
  implicit lazy val UserOrgWrites: Writes[UserOrg] = Writes[UserOrg] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "capacity" -> Json.toJson(o.capacity)).filter(_._2 != JsNull))
  }
  implicit lazy val CredentialsReads: Reads[Credentials] = Reads[Credentials] {
    json => JsSuccess(Credentials((json \ "username").asOpt[String], (json \ "password").asOpt[String]))
  }
  implicit lazy val CredentialsWrites: Writes[Credentials] = Writes[Credentials] {
    o => JsObject(Seq("username" -> Json.toJson(o.username), "password" -> Json.toJson(o.password)).filter(_._2 != JsNull))
  }
  implicit lazy val DashboardIframesReads: Reads[DashboardIframes] = Reads[DashboardIframes] {
    json => JsSuccess(DashboardIframes((json \ "iframe_url").asOpt[String], (json \ "origin").asOpt[String], (json \ "title").asOpt[String]))
  }
  implicit lazy val DashboardIframesWrites: Writes[DashboardIframes] = Writes[DashboardIframes] {
    o => JsObject(Seq("iframe_url" -> Json.toJson(o.iframe_url), "origin" -> Json.toJson(o.origin), "title" -> Json.toJson(o.title)).filter(_._2 != JsNull))
  }
  implicit lazy val ErrorReads: Reads[Error] = Reads[Error] {
    json => JsSuccess(Error((json \ "code").asOpt[Int], (json \ "message").asOpt[String], (json \ "fields").asOpt[String]))
  }
  implicit lazy val ErrorWrites: Writes[Error] = Writes[Error] {
    o => JsObject(Seq("code" -> Json.toJson(o.code), "message" -> Json.toJson(o.message), "fields" -> Json.toJson(o.fields)).filter(_._2 != JsNull))
  }
  implicit lazy val SuccessReads: Reads[Success] = Reads[Success] {
    json => JsSuccess(Success((json \ "message").asOpt[String], (json \ "fields").asOpt[String]))
  }
  implicit lazy val SuccessWrites: Writes[Success] = Writes[Success] {
    o => JsObject(Seq("message" -> Json.toJson(o.message), "fields" -> Json.toJson(o.fields)).filter(_._2 != JsNull))
  }
  implicit lazy val DashboardReads: Reads[Dashboard] = Reads[Dashboard] {
    json => JsSuccess(Dashboard((json \ "user").asOpt[String], (json \ "layout").asOpt[String], (json \ "status").asOpt[String]))
  }
  implicit lazy val DashboardWrites: Writes[Dashboard] = Writes[Dashboard] {
    o => JsObject(Seq("user" -> Json.toJson(o.user), "layout" -> Json.toJson(o.layout), "status" -> Json.toJson(o.status)).filter(_._2 != JsNull))
  }
}
