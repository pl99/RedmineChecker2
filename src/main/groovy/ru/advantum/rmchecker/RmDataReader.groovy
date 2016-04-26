package ru.advantum.rmchecker


import com.rethinkdb.RethinkDB
import com.rethinkdb.net.Connection
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.logging.log4j.core.layout.JacksonFactory
import sun.nio.ch.ThreadPool

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


/**
 * Created by Kotin on 17.02.2016.
 */

@Slf4j
class RmDataReader {
    ConfigObject config
    ConfigObject mailerConfig
    final Collection<String> interestedNames = ["status_id", "priority_id", "tracker_id"]
    final Mailer mailer
    RESTClient restClient
    ArrayList<RmNotifierStruct> idsArray = new ArrayList<>()
    final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    final Long currentTime = new Date().getTime()

//    RethinkDB rdb = RethinkDB.r
//    Connection rDbconn


    RmDataReader(ConfigObject config) {
        this.config = config
        mailerConfig = this.config.mailer
        restClient = new RESTClient(config.redmine_url)
        restClient.ignoreSSLIssues()

        restClient.auth.basic(config.redmine_login, config.redmine_password)
        formatter.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
        mailer = new Mailer(mailerConfig/*, threadPool*/)
        Map cfg = new HashMap()
        cfg.putAll(config)
        cfg.put("password", "***")
        cfg.remove("mailer")
        log.info "Full config=" + cfg.toString()

    }


    ArrayList<RmNotifierStruct> GetIssuesList() {

        config.projects.each { p ->

            try {
                HttpResponseDecorator resp = restClient.get(path: '/issues.json'
                        , query: p.query_parm
                        , contentType: ContentType.JSON
                        , headers: [Accept: 'application/json']
                )
                if (resp.status == 200) {
                    if (resp.data.issues?.id.size() > 0) Thread.start {
//                        log.info ("---------------")
//                        log.info resp.data.issues.updated_on.toString()
//                        log.info(p.time_delay.toString())
//                        log.info("${restClient.uri}/issues/${resp.data.issues?.id}")

                        resp.data.issues.findAll{TimeUnit.MILLISECONDS.toHours(currentTime - formatter.parse(it.updated_on).getTime()) > p.time_delay}.each{i->
                            RmNotifierStruct rns = new RmNotifierStruct(i.id, p.time_delay, p.address)
                            Date updatedOn = formatter.parse(i.updated_on)
                            Long diff = TimeUnit.MILLISECONDS.toHours(currentTime -  updatedOn.getTime() /*formatter.parse(i.updated_on).getTime()*/)

                            rns.setIssueUrl("${restClient.uri}/issues/${i.id}")
                            rns.setDiff(diff)
                            rns.setSubject(i.subject)
                            rns.setAssigned_to_name(i.assigned_to?.name)
                            rns.setLastChanged(updatedOn)

                            log.info(rns.toString())

                            idsArray.add(rns)
                        }
//                        old version here
//                        resp.data.issues.id.each {
//                            idsArray.add(new RmNotifierStruct(it, p.time_delay, p.address))
//                        }
                    }
                } else {
                    log.error "Got response: ${resp.statusLine}"
                }
            }
            catch (ex) {
                log.error "Got response: ${ex.message}"

            }
        }

        return idsArray
    }

    ArrayList<RmNotifierStruct>  GetJournal() {
        idsArray.each { RmNotifierStruct iaRns ->
            try {
                def resp = restClient.get(path: "/issues/${iaRns.issue_id}.json"
                        , query: ["include": "journals"]
                        , contentType: ContentType.JSON
                        , headers: [Accept: 'application/json']
                )
                if (resp.status == 200) {
                    /*def threadedClosure =*/  Thread.start {
                        Date lastChanged
                        def dt = resp.data.issue?.journals?.created_on ?: resp.data.issue?.updated_on ?: resp.data.issue?.created_on
                        if (dt instanceof ArrayList) {
                            lastChanged = formatter.parse(dt.sort()[0])
                        } else {
                            if (dt instanceof Date) {
                                lastChanged = dt
                            }
                        }
                        log.info "START CHANGED ${lastChanged} for ${restClient.uri}/issues/${iaRns.issue_id}.json?include=journals "
                        resp.data?.issue?.journals.each { j ->
                            Collection<String> names = j.details.name
                            if (!Collections.disjoint(interestedNames, names)) {
                                if (lastChanged < formatter.parse(j.created_on)) {
                                    lastChanged = formatter.parse(j.created_on)
                                }
                            }
                        }

                        log.info "LAST CHANGED ${lastChanged} for ${restClient.uri}/issues/${iaRns.issue_id}.json?include=journals "
                        Long diff = TimeUnit.MILLISECONDS.toHours(currentTime - lastChanged.getTime())
                        log.info "difference = ${diff}, delay=${iaRns.time_delay}  for ${restClient.uri}/issues/${iaRns.issue_id}.json?include=journals "

                        if (iaRns.time_delay <= diff) {
                            String subject = resp.data.issue.subject
                            log.info subject
                            GString content = """Внимание к задаче ${config.redmine_url}/issues/${iaRns.issue_id}
С момента последнего обновления статуса/трекера/приоритета прошло уже ${diff} час.
Назначена на ${resp.data.issue?.assigned_to?.name}
Последнее обновление ${lastChanged}

Это сообщение отправлено автоматически. Не отвечайте на него
"""
                            iaRns.setIssueUrl("${restClient.uri}/issues/${iaRns.issue_id}")
                            iaRns.setDiff(diff)
                            iaRns.setSubject(subject)
                            iaRns.setAssigned_to_name(resp.data.issue.assigned_to?.name)
                            iaRns.setLastChanged(lastChanged)

//                            log.info content
                           // mailer.SendMail(subject, content, iaRns.address)
                        }
                    }

                } else {
                    log.error "${restClient.uri}/issues/${iaRns.issue_id}.json?include=journals"
                    log.error "Got response: ${resp.statusLine}"
                }
            }
            catch (ex) {
                log.error "Got response: ${ex.message}"
            }
        }
        return idsArray
    }
}

