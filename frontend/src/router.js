import { createRouter, createWebHistory } from 'vue-router'
import Home from "@/views/Home.vue"
import UploadImage from "@/views/UploadImage.vue"

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: Home
        },
        {
            path: '/upload-image',
            component: UploadImage
        }
    ]
})

export default router