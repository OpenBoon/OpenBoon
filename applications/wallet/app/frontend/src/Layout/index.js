import PropTypes from 'prop-types'

import LayoutNavBar from './NavBar'

const Layout = ({ children }) => {
  return (
    <div css={{ height: '100%' }}>
      <LayoutNavBar />
      {children}
    </div>
  )
}

Layout.propTypes = {
  children: PropTypes.node.isRequired,
}

export default Layout
