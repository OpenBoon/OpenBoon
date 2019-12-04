import PropTypes from 'prop-types'
import { colors, constants, spacing, typography } from '../Styles/'

const ROW_HEIGHT = 32

const tdCSS = {
  height: ROW_HEIGHT,
  fontWeight: typography.weight.extraLight,
  color: colors.grey2,
  padding: `${spacing.base}px ${spacing.normal}px`,
  border: `${constants.borderWidth.default}px solid transparent`,
}

const Table = ({ columns, rows }) => {
  const thCSS = {
    height: ROW_HEIGHT,
    fontWeight: typography.weight.extraLight,
    color: colors.grey2,
    backgroundColor: colors.grey1,
    padding: `${spacing.moderate}px ${spacing.normal}px`,
    borderBottom: `${constants.borderWidth.default}px solid ${colors.grey5}`,
    '&:not(:last-child)': {
      borderRight: `${constants.borderWidth.default}px solid ${colors.grey5}`,
    },
  }

  const trCSS = {
    backgroundColor: colors.grey4,
    '&:nth-of-type(2n)': {
      backgroundColor: colors.grey3,
    },
  }

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        padding: spacing.normal,
      }}>
      <table
        css={{
          borderSpacing: 0,
          boxShadow: constants.boxShadows.dark,
          margin: `${spacing.base}px 0`,
        }}>
        <thead>
          <tr css={trCSS}>
            {columns.map(column => {
              return (
                <th key={column} css={thCSS}>
                  <div css={{ display: 'flex' }}>{column}</div>
                </th>
              )
            })}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => {
            return (
              <tr key={`row-${index}`} css={trCSS}>
                {row.map((cell, index) => {
                  return (
                    <td key={`cell-${index}`} css={tdCSS}>
                      {cell}
                    </td>
                  )
                })}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

Table.propTypes = {
  columns: PropTypes.array.isRequired,
  rows: PropTypes.arrayOf(PropTypes.array).isRequired,
}

export default Table
