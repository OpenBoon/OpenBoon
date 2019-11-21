import PropTypes from 'prop-types'
import React from 'react'

function Page({ children }) {
  return <div className="Page">{children}</div>
}

Page.propTypes = {
  children: PropTypes.object,
}

Page.defaultProps = {
  children: {},
}

export default Page
