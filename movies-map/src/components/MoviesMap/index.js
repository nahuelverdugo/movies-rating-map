import React, {Component} from "react"
import {ComposableMap, ZoomableGroup, Geographies, Geography} from "react-simple-maps"
import {scaleLinear} from "d3-scale"
import ReactTooltip from "react-tooltip"
import worldMap from "static/world-50m.json"
import data from "static/data.json"
import github from "static/github.png"

class MoviesMap extends Component {
  constructor(props) {
    super(props);
    this.state = {
      selectedCountry: "ESP",
      selectedCountryName: "Spain",
      avgRatingReceived: data["ESP"].avgRatingReceived,
      avgRatingGiven: data["ESP"].avgRatingGiven,
      votes: data["ESP"].votesGiven,
      films: data["ESP"].films,
      avg: data["ESP"].avgRatingReceived,
      show: "given",
      useAvg: false,
      countries: data["ESP"].ratingGiven
    }
    this.clickCountry = this.clickCountry.bind(this);
    this.changeAvg = this.changeAvg.bind(this);
    this.changeShow = this.changeShow.bind(this);
  }
  componentDidMount() {
    setTimeout(() => {
      ReactTooltip.rebuild()
    }, 100);
  }
  componentWillUnmount() {
    window.removeEventListener("resize", this.updateScaleHeight);
  }
  changeAvg(evt) {
    this.setState({useAvg: evt.target.checked});
  }
  changeShow(evt) {
    let countries = (evt.target.value === "given")
      ? data[this.state.selectedCountry].ratingGiven
      : data[this.state.selectedCountry].ratingReceived;
    this.setState({
      show: evt.target.value,
      countries: countries != null
        ? countries
        : [],
      avg: (evt.target.value === "given")
        ? data[this.state.selectedCountry].avgRatingGiven
        : data[this.state.selectedCountry].avgRatingReceived,
      votes: (evt.target.value === "given")
        ? data[this.state.selectedCountry].votesGiven
        : data[this.state.selectedCountry].votesReceived
    });
  }
  getScaleStyle() {
    return {
      WebkitTransition: "all",
      msTransition: "all",
      backgroundImage: `linear-gradient(to bottom, rgb(47, 204, 64) 0%, rgb(255, 220, 0) ${this.state.useAvg
        ? 100 - (this.state.avg * 10)
        : "50"}%, rgb(255, 65, 45) 100%)`
    };
  }
  clickCountry(geography, evt) {
    let countries = (this.state.show === "given")
      ? data[geography.id].ratingGiven
      : data[geography.id].ratingReceived;
    this.setState({
      selectedCountry: geography.id,
      selectedCountryName: geography.properties.name,
      avgRatingReceived: data[geography.id].avgRatingGiven,
      avgRatingGiven: data[geography.id].avgRatingReceived,
      films: data[geography.id].films,
      countries: countries != null
        ? countries
        : [],
      avg: (this.state.show === "given")
        ? data[geography.id].avgRatingGiven
        : data[geography.id].avgRatingReceived,
      votes: (this.state.show === "given")
        ? data[geography.id].votesGiven
        : data[geography.id].votesReceived
    });
  }
  render() {
    const {
      useAvg,
      show,
      selectedCountry,
      countries,
      avgRatingReceived,
      avgRatingGiven,
      avg,
      votes,
      ...rest
    } = this.state;
    const colorScale = scaleLinear().domain([
      1, useAvg
        ? avg
        : 5.5,
      10
    ]).range(["#FF412D", "#FFDC00", "#2FCC40"]);
    return (<div className="container">
      <div className="options">
        <i>OPTIONS</i>
        SHOW RATING:
        <input type="radio" name="show" defaultChecked={true} value="given" onChange={this.changeShow}/>
        GIVEN
        <input type="radio" name="show" defaultChecked={false} value="received" onChange={this.changeShow}/>
        RECEIVED
        <input type="checkbox" onChange={this.changeAvg}/>
        USE AVG.
      </div>
      <div className="map-area">
        <div className="movies-map">
          <ComposableMap projectionConfig={{
              scale: 205
            }} width={980} height={551} style={{
              width: "100%",
              height: "auto"
            }}>
            <Geographies geography={worldMap} disableOptimization="disableOptimization">
              {
                (geographies, projection) => geographies.map((geography, i) => {
                  if (geography.id !== "ATA") {
                    let countryData = this.state.countries[geography.id] != null
                      ? this.state.countries[geography.id].toFixed(2)
                      : null;
                    let colour = this.state.countries[geography.id] != null
                      ? colorScale(countries[geography.id])
                      : "#EEEEEE";
                    return (<Geography key={i} data-tip={geography.properties.name + (
                        countryData != null
                        ? ": " + countryData + (
                          votes[geography.id] != null
                          ? " - # ratings: " + votes[geography.id]
                          : "")
                        : "")} geography={geography} projection={projection} onClick={data[geography.id] != null
                        ? this.clickCountry
                        : undefined} style={{
                        default: {
                          fill: colour,
                          stroke: "#36393D",
                          strokeWidth: geography.id == selectedCountry
                            ? 2
                            : 0.75,
                          outline: "none"
                        },
                        hover: {
                          fill: colour,
                          stroke: "#36393D",
                          strokeWidth: geography.id == selectedCountry
                            ? 2
                            : 0.75,
                          outline: "none"
                        },
                        pressed: {
                          fill: colour,
                          stroke: "#36393D",
                          strokeWidth: geography.id == selectedCountry
                            ? 2
                            : 0.75,
                          outline: "none"
                        }
                      }}/>)
                  }
                })
              }
            </Geographies>
          </ComposableMap>
          <ReactTooltip className="extraClass"/>
        </div>
        <div className="scale" style={this.getScaleStyle()} ref={(el) => this.scale = el}></div>
        <div className="scale-numbers">
          {[...Array(10).keys()].map(e => (<div key={e} className="scale-number">{10 - e}</div>))}
        </div>
      </div>
      <div className="controls">
        SELECTED COUNTRY:
        <i>{this.state.selectedCountryName.toUpperCase()}</i>
        ({this.state.films} films) | AVG. RATING GIVEN:
        <i>{
            this.state.avgRatingGiven > 0
              ? this.state.avgRatingGiven.toFixed(2)
              : "-"
          }</i>| AVG. RATING RECEIVED:
        <i>{
            this.state.avgRatingReceived > 0
              ? this.state.avgRatingReceived.toFixed(2)
              : "-"
          }</i>
        <div className="info">
          SOURCE:
          <a href="https://www.filmaffinity.com/es/">fillAffinity/es</a>
        </div>
      </div>
      <div className="link">
        <a href="https://github.com/nahuelvr/movie-rating-map">
          <img width={24} src={github}/>
          <span>movie-rating-map</span>
        </a>
      </div>
    </div>)
  }
}

export default MoviesMap
