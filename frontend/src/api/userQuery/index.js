import request from '@/utils/request'

export function getUserInfo(data) {
  return request({
    url: '/user/query/userInfo',
    method: 'post',
    loading: false,
    data
  })
}

export function getUserEventNum(data) {
  return request({
    url: '/user/query/userEventNum',
    method: 'post',
    loading: false,
    data
  })
}

export function getUserEventDetail(data) {
  return request({
    url: '/user/query/userEventDetail',
    method: 'post',
    loading: false,
    data
  })
}

export function getUserEventDetailForDate(data) {
  return request({
    url: '/user/query/userEventDetailForDate',
    method: 'post',
    loading: false,
    data
  })
}

export function getUserList(data) {
  return request({
    url: '/user/query/userList',
    method: 'post',
    loading: false,
    data
  })
}

export function getOptions(data) {
  return request({
    url: '/user/query/options',
    method: 'post',
    loading: false,
    data
  })
}

export default {
  getOptions,
  getUserInfo,
  getUserEventNum,
  getUserEventDetail,
  getUserEventDetailForDate,
  getUserList
}
