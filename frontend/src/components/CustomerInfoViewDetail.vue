<template>

    <v-card outlined>
        <v-card-title>
            CustomerInfo # {{$route.params.id }}
        </v-card-title>

        <v-card-text>
            <div>
                <Number label="CustomerId" v-model="item.customerId" :editMode="editMode" @change="change" />
            </div>
            <div>
                <Number label="FlightCount" v-model="item.flightCount" :editMode="editMode" @change="change" />
            </div>
            <div>
                <Number label="RecentReserveDate" v-model="item.recentReserveDate" :editMode="editMode" @change="change" />
            </div>
        </v-card-text>
    </v-card>

</template>

<script>
  const axios = require('axios').default;

  export default {
    name: 'CustomerInfoViewDetail',
    props: {
      value: Object,
    },
    data: () => ({
        item : [],
    }),
    async created() {
      var params = this.$route.params;
      var temp = await axios.get(axios.fixUrl('/customerInfos/' + params.id))

      this.item = temp.data;

    },
    methods: {
    }
  }
</script>

