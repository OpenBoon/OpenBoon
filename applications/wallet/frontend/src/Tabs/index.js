import PropTypes from 'prop-types'

import { spacing, constants } from '../Styles'

import TabsLink from './Link'

const Tabs = ({ tabs }) => {
  return (
    <nav css={{ padding: spacing.normal, paddingLeft: 0, paddingRight: 0 }}>
      <ul
        css={{
          listStyleType: 'none',
          padding: 0,
          margin: 0,
          display: 'flex',
          borderBottom: constants.borders.regular.iron,
        }}
      >
        {tabs
          .filter(({ title, href }) => title && href)
          .map(({ title, href }) => (
            <TabsLink key={href} title={title} href={href} />
          ))}
      </ul>
    </nav>
  )
}

Tabs.propTypes = {
  tabs: PropTypes.arrayOf(
    PropTypes.shape({
      title: PropTypes.string,
      href: PropTypes.string,
    }).isRequired,
  ).isRequired,
}

export default Tabs
