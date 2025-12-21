import request from '@/utils/request'

export function registerArchive(data) {
    return request({ url: '/api/archive/register', method: 'post', data })
}
export function getReviewList() {
    return request({ url: '/api/archive/list/review', method: 'get' })
}
export function passArchive(id) {
    return request({ url: `/api/archive/review/${id}`, method: 'post' })
}
export function deleteArchive(id) {
    return request({ url: `/api/archive/${id}`, method: 'delete' })
}