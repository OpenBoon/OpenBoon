import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

const Table = ({ legend, columns, items, renderRow }) => {
  return (
    <div css={{ position: 'relative' }}>
      <div
        css={{
          position: 'absolute',
          right: 0,
          top: -spacing.normal * 2,
          color: colors.structure.zinc,
        }}>
        {legend}
      </div>
      <table
        css={{
          width: '100%',
          borderSpacing: 0,
          boxShadow: constants.boxShadows.table,
          whiteSpace: 'nowrap',
          tr: {
            backgroundColor: colors.structure.lead,
            '&:nth-of-type(2n)': {
              backgroundColor: colors.structure.mattGrey,
            },
            ':hover': {
              backgroundColor: colors.structure.iron,
              boxShadow: constants.boxShadows.tableRow,
              td: {
                border: constants.borders.tableRow,
                borderLeft: '0',
                borderRight: '0',
                '&:first-of-type': {
                  borderLeft: constants.borders.tableRow,
                },
                '&:last-of-type': {
                  borderRight: constants.borders.tableRow,
                },
              },
            },
          },
          td: {
            fontWeight: typography.weight.extraLight,
            color: colors.structure.pebble,
            padding: `${spacing.base}px ${spacing.normal}px`,
            border: constants.borders.transparent,
            borderLeft: '0',
            borderRight: '0',
            ':first-of-type': {
              borderLeft: constants.borders.transparent,
            },
            ':last-of-type': {
              borderRight: constants.borders.transparent,
            },
          },
        }}>
        <thead>
          <tr>
            {columns.map(column => (
              <th
                key={column}
                css={{
                  textAlign: 'left',
                  fontSize: typography.size.kilo,
                  lineHeight: typography.height.kilo,
                  fontWeight: typography.weight.medium,
                  color: colors.structure.pebble,
                  backgroundColor: colors.structure.iron,
                  padding: `${spacing.moderate}px ${spacing.normal}px`,
                  borderBottom: constants.borders.default,
                  ':nth-of-type(2)': { width: '100%' },
                  '&:not(:last-child)': {
                    borderRight: constants.borders.default,
                  },
                }}>
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{items.map(item => renderRow(item))}</tbody>
      </table>
    </div>
  )
}

Table.propTypes = {
  legend: PropTypes.string.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string).isRequired,
  items: PropTypes.arrayOf(PropTypes.object).isRequired,
  renderRow: PropTypes.func.isRequired,
}

export default Table
