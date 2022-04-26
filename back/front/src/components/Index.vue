<template>
  <div>
    <div>
      <el-form ref="form" :model="sqlForm" label-width="80px">
        <el-form-item>
          <el-input
              type="textarea"
              :autosize="{ minRows: 4, maxRows: 10 }"
              placeholder="请输入SQL查询语句，多次查询点Reset！！！"
              v-model="sqlForm.sql">
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSubmit">Analyze</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reset">Reset</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="container">

    </div>
  </div>
</template>

<script>

import * as d3 from 'd3'

export default {
  name: "Index",
  data() {
    return {
      sqlForm: {
        sql: ''
      },
      testGraph: {
        "nodes": [],
        "links": []
      },
      width: 1200,
      height: 800,
      // colorList: ['red', 'blue'],
      svgArea: null,
      // links: [],
      // nodes: [],
      errorMessage: ''
    }
  },
  mounted() {
    // var _this = this;
    // this.request.get("/search/username(temp)")
    // this.request.get("/all")
    //     .then(function (response) {
    //       console.log(response)
    //       _this.testGraph["nodes"] = response.nodes;
    //       _this.testGraph["links"] = response.links;
    //       _this.initGraph(_this.testGraph)
    //       // console.log("lalala")
    //     })
    //     .catch(function (error) {
    //       console.log(error)
    //     })
  },
  methods: {
    reset() {
      this.sqlForm.sql = "";
      location.reload();
      // 吐了，没找到d3怎么删元素，网上的remove没用
    },
    onSubmit() {
      var _this = this
      if (_this.sqlForm.sql === "") {
        const h = _this.$createElement;

        _this.$notify({
          title: 'EMPTY SQL',
          message: h('i', { style: 'color: teal'}, '')
        })
        return
      }
      this.request.post("/analyze", this.sqlForm.sql)
          .then(function (response) {
            console.log(response)
            if(response.error===""){
              _this.testGraph["nodes"] = response.nodes;
              _this.testGraph["links"] = response.links;
              _this.initGraph(_this.testGraph)
            }
            else{
              const h = _this.$createElement;

              _this.$notify({
                title: 'WRONG SQL',
                message: h('i', { style: 'color: teal'}, '')
              });
            }
          })
          .catch(function (error) {
            console.log(error)
          })
    },
    color(d) {
      // const scale = d3.scaleOrdinal(d3.schemeCategory10);
      // return d => scale(d.group);
      return '#3388B1';
    },
    drag(simulation) {
      function dragstarted(event) {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        event.subject.fx = event.subject.x;
        event.subject.fy = event.subject.y;
      }

      function dragged(event) {
        event.subject.fx = event.x;
        event.subject.fy = event.y;
      }

      function dragended(event) {
        if (!event.active) simulation.alphaTarget(0);
        event.subject.fx = event.x;
        event.subject.fy = event.y;
      }

      return d3.drag()
          .on("start", dragstarted)
          .on("drag", dragged)
          .on("end", dragended);
    },
    initGraph(data) {
      var _this = this;
      const links = data.links.map(d => Object.create(d));
      const nodes = data.nodes.map(d => Object.create(d));

      const simulation = d3.forceSimulation(nodes)
          .force("link", d3.forceLink(links).id(d => d.name))
          // .force("link", d3.forceLink(links).id(d => d.name).distance(30))
          // .force("collide", d3.forceCollide().radius(() => 20))
          // .force("charge", d3.forceManyBody().strength(-10))
          .force("center", d3.forceCenter(_this.width / 2, _this.height / 2));

      // const svg = d3.create("svg")
      //     .attr("viewBox", [0, 0, _this.width, _this.height]);

      // 缩放
      const zoom = d3.zoom()
          .on("zoom", (event) => {
            g.attr("transform", event.transform);
          })
          .scaleExtent([1, 40]);

      this.svgArea = d3.select(".container")
          .append("svg")
          .attr("viewBox", [0, 0, _this.width, _this.height])
          .call(zoom);

      // this.svgArea = d3.select(".container")
      //     .append("svg")
      //     .attr("viewBox", [0, 0, this.width, this.height])
      //     .call(d3.zoom().on("zoom",function () {
      //       g.attr("transform",d3.event.transform)
      //     }))

      // 箭头
      const marker = this.svgArea.append("marker")
          .attr("id", "direction")
          .attr("orient", "auto")
          .attr("stroke-width", 2)
          .attr("markerUnits", "strokeWidth")
          .attr("markerUnits", "userSpaceOnUse")
          .attr("viewBox", "0 -5 10 10")
          .attr("refX", 22) //偏移
          .attr("refY", 0)
          .attr("markerWidth", 12)
          .attr("markerHeight", 12)
          .append("path")
          .attr("d", "M 0 -5 L 10 0 L 0 5")
          .attr('fill', '#999')
          .attr("stroke-opacity", 0.6);


      const g = this.svgArea.append("g")
          .attr("class", "content");

      const link = g.append("g")
          .attr("stroke", "#999")
          .attr("stroke-opacity", 0.6)
          .attr("marker-end", "url(#direction)")
          .selectAll("path")
          .data(links)
          .join("path")
          .attr("stroke-width", d => Math.sqrt(d.value))
          .attr("class", "link");

      const node = g.append("g")
          .attr("stroke", "#fff")
          .attr("stroke-width", 1)
          .selectAll("circle")
          .data(nodes)
          .join("circle")
          .attr("r", 15)
          .attr("fill", _this.color)
          .call(_this.drag(simulation))
          .attr("class", "node");

      node.append("title")
          .text(d => d.name);

      const nodeText = g.append("g")
          .selectAll("text")
          .data(nodes)
          .join("text")
          .text(function (d) {
            return d.name;
          })
          .attr("dx", function () {
            return this.getBoundingClientRect().width / 4 * (-1);
          })
          .attr("dy", 40)
          .attr("class", "nodeName")
          .attr("fill", "white");
      // console.log(nodeText)

      simulation.on("tick", () => {
        link
            // .attr("x1", d => d.source.x)
            // .attr("y1", d => d.source.y)
            // .attr("x2", d => d.target.x)
            // .attr("y2", d => d.target.y);
            .attr("d", d => "M " + d.source.x + " " + d.source.y + " L " + d.target.x + " " + d.target.y);

        node
            .attr("cx", d => d.x)
            .attr("cy", d => d.y);

        nodeText
            .attr("x", d => d.x)
            .attr("y", d => d.y);
      });
    },
    updateGraph(data){
      var _this = this;
      const links = data.links.map(d => Object.create(d));
      const nodes = data.nodes.map(d => Object.create(d));
    }
  }
}
</script>

<style scoped>

body {
  margin: 0px;
}

.container {
  width: 1200px;
  height: 800px;
  border: 1px solid #2c3e50;
  border-radius: 8px;
  margin-top: 40px;
  margin-left: auto;
  margin-right: auto;
  background: #154360 repeating-linear-gradient(30deg,
  hsla(0, 0%, 100%, .1), hsla(0, 0%, 100%, .1) 15px,
  transparent 0, transparent 30px);
}

.node {
  stroke: #fff;
  stroke-width: 1;
  cursor: pointer;
}

.node:hover {
  stroke-width: 5;
}

.nodeName {
  fill: white;
}
</style>