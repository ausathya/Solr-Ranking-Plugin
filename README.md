## Welcome to Solr Ranking Plugin !
### A Solr plugin that computes rank for variety of ranking strategies.

  Plugin has two primary components, A **Rank Component**, that extends Solr Search component and uses the results returned by to compute the rank. Rank component internally depends on Solr's search & facet components. Second, Simple **Rank Engine** that implements various ranking strategies. **Supported ranking strategies** are **Dense**, **Standard Competition**, **Modified Competition**, **Fractional** & **Ordinal**. Good explanation of ranking strategies can be found at [Wikipedia](http://en.wikipedia.org/wiki/Ranking)

_This plugin itself very light-weight, performance of this plugin is directly tied to how well Lucene can Sort & handle Deep Pagging & how well your schema is defined. Solr/Lucene 3.5 offers significant performance improvements for Deep Paging._

***
### License:
   Project is license under **Apache License 2.0**. [Apache License](http://www.apache.org/licenses/LICENSE-2.0.html). More details on the project license & other intellectual property dependencies please refer to [[License|License]]

***
### Latest Release:
   Latest release is 0.2.0. Please download the latest release from [Downloads](https://github.com/ausathya/Solr-Ranking-Plugin/downloads)

***
### Further reading
 * [[Getting Started|Getting Started]]
 * [[Dependencies|Dependencies]]
 * [[How it got started ?|How it got started ?]]
 * [[Guide - Solr 1.4|Guide - Solr 1.4]]
 * [[License|License]]

***