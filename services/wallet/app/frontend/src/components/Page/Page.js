import PropTypes from 'prop-types'
import React, { Component } from 'react'

class Page extends Component {
  render() {
    return <div className="page">{this.props.children}</div>
  }
}

Page.propTypes = {
  children: PropTypes.object,
}

export default Page
