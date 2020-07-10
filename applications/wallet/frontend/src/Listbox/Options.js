import PropTypes from 'prop-types'
import { ListboxOption } from '@reach/listbox'

import listboxShape from './shape'

import { colors, spacing, typography } from '../Styles'

const ListboxOptions = ({ options, nestedCount }) => {
  return Object.keys(options).map((key) => {
    const value = options[key]

    if (typeof value === 'string') {
      return (
        <ul key={value} css={{ padding: 0 }}>
          <ListboxOption
            value={value}
            css={{
              paddingTop: spacing.mini,
              paddingBottom: spacing.mini,
              paddingLeft:
                spacing.comfy +
                (nestedCount > 0 ? (nestedCount - 1) * spacing.normal : 0),
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              fontWeight: typography.weight.regular,
              ':hover': {
                background: colors.structure.zinc,
                cursor: 'pointer',
              },
            }}
          >
            {key}
          </ListboxOption>
        </ul>
      )
    }

    return (
      <ul
        key={key}
        aria-labelledby={key}
        css={{
          listStyle: 'none',
          paddingLeft: 0,
          paddingBottom: spacing.base,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.medium,
        }}
        role="group"
      >
        <li css={{ paddingBottom: spacing.mini }} role="presentation">
          <span
            css={{ paddingLeft: spacing.base + nestedCount * spacing.normal }}
          >
            {key}
          </span>
          <ListboxOptions options={value} nestedCount={nestedCount + 1} />
        </li>
      </ul>
    )
  })
}

ListboxOptions.defaultProps = {
  nestedCount: 0,
}

ListboxOptions.propTypes = {
  options: listboxShape.isRequired,
  nestedCount: PropTypes.number,
}

export default ListboxOptions
