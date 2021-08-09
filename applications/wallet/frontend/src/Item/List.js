import PropTypes from 'prop-types'

import { typography, colors, spacing } from '../Styles'

import ButtonCopy from '../Button/Copy'

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
      {attributes.map(([label, value, copyButtonTitle]) => {
        return (
          <li key={label} css={{ display: 'flex', alignItems: 'center' }}>
            <div
              css={{
                color: colors.structure.zinc,
                fontFamily: typography.family.condensed,
                textTransform: 'uppercase',
                paddingRight: spacing.base,
              }}
            >
              {label}:
            </div>

            <div css={{ display: 'flex', alignItems: 'center' }}>
              {value}

              {!!copyButtonTitle && (
                <div css={{ paddingLeft: spacing.small }}>
                  <ButtonCopy
                    title={copyButtonTitle}
                    value={value}
                    offset={50}
                  />
                </div>
              )}
            </div>
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
