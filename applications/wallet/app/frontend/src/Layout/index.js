import PropTypes from 'prop-types'

import NavBar from './NavBar'

const Layout = ({ children }) => {
  return (
    <div css={{ height: '100%' }}>
      <NavBar />
      {children}
    </div>
  )
}

Layout.propTypes = {
  children: PropTypes.node.isRequired,
}

export default Layout
