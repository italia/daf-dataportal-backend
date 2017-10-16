package it.gov.daf.datipubblici

case class DatasetTest(test_multi: Option[String], groups: Option[List[Group]])

case class Catalog(name: Option[String])

case class Distribution(label: Option[String], count: Option[Float])

case class BrokenLink(label: Option[String], url: Option[String], m_status: Option[String], name: Option[String], rurl: Option[String], dataset_url: Option[String], catalog_name: Option[String])

case class Dataset(alternate_identifier: Option[String], author: Option[String], author_email: Option[String], frequency: Option[String], groups: Option[List[Group]], holder_identifier: Option[String], holder_name: Option[String], identifier: Option[String], license_id: Option[String], license_title: Option[String], modified: Option[String], name: Option[String], notes: Option[String], organization: Option[Organization], owner_org: Option[String], publisher_identifier: Option[String], publisher_name: Option[String], relationships_as_object: Option[List[Relationship]], relationships_as_subject: Option[List[Relationship]], resources: Option[List[Resource]], tags: Option[List[Tag]], theme: Option[String])

case class Group(display_name: Option[String], description: Option[String], image_display_url: Option[String], title: Option[String], id: Option[String], name: Option[String])

case class Organization(approval_status: Option[String], created: Option[String], description: Option[String], email: Option[String], id: Option[String], image_url: Option[String], is_organization: Option[Boolean], name: Option[String], revision_id: Option[String], state: Option[String], title: Option[String], `type`: Option[String], users: Option[List[UserOrg]])

case class Relationship(subject: Option[String], `object`: Option[String], `type`: Option[String], comment: Option[String])

case class Resource(cache_last_updated: Option[String], cache_url: Option[String], created: Option[String], datastore_active: Option[Boolean], description: Option[String], distribution_format: Option[String], format: Option[String], hash: Option[String], id: Option[String], last_modified: Option[String], mimetype: Option[String], mimetype_inner: Option[String], name: Option[String], package_id: Option[String], position: Option[Int], resource_type: Option[String], revision_id: Option[String], size: Option[Int], state: Option[String], url: Option[String])

case class Tag(display_name: Option[String], id: Option[String], name: Option[String], state: Option[String], vocabulary_id: Option[String])

case class User(name: Option[String], email: Option[String], password: Option[String], fullname: Option[String], about: Option[String])

case class UserOrg(name: Option[String], capacity: Option[String])

case class Credentials(username: Option[String], password: Option[String])

case class DashboardIframes(iframe_url: Option[String], origin: Option[String], title: Option[String])

case class Error(code: Option[Int], message: Option[String], fields: Option[String])

case class Success(message: Option[String], fields: Option[String])

case class Dashboard(user: Option[String], layout: Option[String], status: Option[String])
