import PropTypes from 'prop-types'
import { colors, constants, spacing, typography } from '../Styles'
import ProgressBar from '../ProgressBar'

const ROW_HEIGHT = 32

const Table = ({ columns, rows }) => {
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
          td: {
            height: ROW_HEIGHT,
            fontWeight: typography.weight.extraLight,
            color: colors.grey2,
            padding: `${spacing.base}px ${spacing.normal}px`,
            border: constants.borders.transparent,
          },
        }}>
        <thead>
          <tr
            css={{
              backgroundColor: colors.grey4,
            }}>
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
              <tr
                key={row.id}
                css={{
                  backgroundColor: colors.grey4,
                  '&:nth-of-type(2n)': {
                    backgroundColor: colors.grey3,
                  },
                  borderSpacing: 0,
                  ':hover': {
                    backgroundColor: colors.grey1,
                    td: {
                      borderSpacing: 0,
                      borderTop: constants.borders.default,
                      borderBottom: constants.borders.default,
                    },
                    'td:first-of-type': {
                      borderTop: constants.borders.default,
                      borderLeft: constants.borders.default,
                      borderBottom: constants.borders.default,
                    },
                    'td:last-of-type': {
                      borderTop: constants.borders.default,
                      borderRight: constants.borders.default,
                      borderBottom: constants.borders.default,
                    },
                  },
                }}>
                <td>{row.status}</td>
                <td>{row.jobName}</td>
                <td>{row.createdBy}</td>
                <td>{row.priority}</td>
                <td>{row.createdDateTime}</td>
                <td>{row.failed}</td>
                <td>{row.errors}</td>
                <td>{row.numAssets}</td>
                <td>
                  <ProgressBar status={row.progress} />
                </td>
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
      progress: PropTypes.shape({
        isGenerating: PropTypes.bool,
        isCanceled: PropTypes.bool,
        canceledBy: PropTypes.string,
        succeeded: PropTypes.number,
        failed: PropTypes.number,
        running: PropTypes.number,
        pending: PropTypes.number,
      }),
    }),
  ).isRequired,
}

export default Table
