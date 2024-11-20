<template>

    <v-data-table
        :headers="headers"
        :items="customerInfo"
        :items-per-page="5"
        class="elevation-1"
    ></v-data-table>

</template>

<script>
    const axios = require('axios').default;

    export default {
        name: 'CustomerInfoView',
        props: {
            value: Object,
            editMode: Boolean,
            isNew: Boolean
        },
        data: () => ({
            headers: [
                { text: "customerId", value: "customerId" },
                { text: "flightCount", value: "flightCount" },
                { text: "recentReserveDate", value: "recentReserveDate" },
            ],
            customerInfo : [],
        }),
          async created() {
            var temp = await axios.get(axios.fixUrl('/customerInfos'))

            temp.data._embedded.customerInfos.map(obj => obj.id=obj._links.self.href.split("/")[obj._links.self.href.split("/").length - 1])

            this.customerInfo = temp.data._embedded.customerInfos;
        },
        methods: {
        }
    }
</script>

