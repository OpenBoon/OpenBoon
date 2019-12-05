import PropTypes from 'prop-types'
import { colors, constants, spacing, typography } from '../Styles'

const ROW_HEIGHT = 32

const Table = ({ columns, rows }) => {
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
          td: {
            height: ROW_HEIGHT,
            fontWeight: typography.weight.extraLight,
            color: colors.grey2,
            padding: `${spacing.base}px ${spacing.normal}px`,
            border: constants.borders.transparent,
          },
        }}>
        <thead>
          <tr css={trCSS}>
            {columns.map(column => {
              return (
                <th
                  key={column}
                  css={{
                    height: ROW_HEIGHT,
                    fontWeight: typography.weight.extraLight,
                    color: colors.grey2,
                    backgroundColor: colors.grey1,
                    padding: `${spacing.moderate}px ${spacing.normal}px`,
                    borderBottom: constants.borders.default,
                    '&:not(:last-child)': {
                      borderRight: constants.borders.default,
                    },
                  }}>
                  <div css={{ display: 'flex' }}>{column}</div>
                </th>
              )
            })}
          </tr>
        </thead>
        <tbody>
          {rows.map(row => {
            return (
              <tr key={row.id} css={trCSS}>
                <td>{row.status}</td>
                <td>{row.jobName}</td>
                <td>{row.createdBy}</td>
                <td>{row.priority}</td>
                <td>{row.createdDateTime}</td>
                <td>{row.failed}</td>
                <td>{row.errors}</td>
                <td>{row.numAssets}</td>
                <td>{row.progress}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

Table.propTypes = {
  columns: PropTypes.arrayOf(PropTypes.string).isRequired,
  rows: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      status: PropTypes.string,
      jobName: PropTypes.string,
      createdBy: PropTypes.string,
      priority: PropTypes.number,
      createdDateTime: PropTypes.number,
      failed: PropTypes.node,
      errors: PropTypes.node,
      numAssets: PropTypes.string,
      progress: PropTypes.node,
    }),
  ).isRequired,
}

export default Table
