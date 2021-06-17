import PropTypes from 'prop-types'

import { typography, colors } from '../Styles'

const LINE_HEIGHT = '23px'

const ItemList = ({ attributes }) => {
  return (
    <ul
      css={{
        margin: 0,
        padding: 0,
        listStyle: 'none',
        lineHeight: LINE_HEIGHT,
      }}
    >
      {attributes.map(([label, value]) => {
        return (
          <li key={label}>
            <span
              css={{
                color: colors.structure.zinc,
                fontFamily: typography.family.condensed,
                textTransform: 'uppercase',
              }}
            >
              {label}:
            </span>{' '}
            {value}
          </li>
        )
      })}
    </ul>
  )
}

ItemList.propTypes = {
  attributes: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string).isRequired)
    .isRequired,
}

export default ItemList
