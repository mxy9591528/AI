import { defineStore } from 'pinia'

export const useAdminStore = defineStore('admin', {
    state: () => ({
        isCollapse: false
    }),
    actions: {
        toggleCollapse() {
            this.isCollapse = !this.isCollapse
        }
    }
})
