package repositories.push_notification

trait Query

final case class SimpleQuery(queryCondition: QueryComponent) extends Query

final case class MultiQuery(seqQueryConditions: Seq[QueryComponent]) extends Query


final case class QueryComponent(nameField: String, valueField: Any)