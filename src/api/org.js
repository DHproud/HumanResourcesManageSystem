import request from '@/utils/request'

export function getOrgList(parentId) {
    return request({ url: `/api/org/list/${parentId}`, method: 'get' })
}