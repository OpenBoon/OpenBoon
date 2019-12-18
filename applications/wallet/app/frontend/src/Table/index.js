import PropTypes from 'prop-types'
import { colors, constants, spacing, typography } from '../Styles'
import { formatFullDate } from '../Date/helpers'
import Status from '../Status'
import ProgressBar from '../ProgressBar'

const BORDER_RADIUS = 32

const Table = ({ columns, rows }) => {
  return (
    <table
      css={{
        width: '100%',
        borderSpacing: 0,
        boxShadow: constants.boxShadows.table,
        whiteSpace: 'nowrap',
        th: {
          fontSize: typography.size.kilo,
          lineHeight: typography.height.kilo,
          fontWeight: typography.weight.medium,
          color: colors.structure.pebble,
          backgroundColor: colors.structure.iron,
          padding: `${spacing.moderate}px ${spacing.normal}px`,
          borderBottom: constants.borders.default,
          ':nth-of-type(2)': {
            width: '100%',
          },
          '&:not(:last-child)': {
            borderRight: constants.borders.default,
          },
        },
        tr: {
          ':hover': {
            backgroundColor: colors.structure.iron,
            boxShadow: constants.boxShadows.tableRow,
            td: {
              border: constants.borders.tableRow,
              borderLeft: '0',
              borderRight: '0',
              '&:first-of-type': {
                border: constants.borders.tableRow,
                borderRight: '0',
              },
              '&:last-of-type': {
                border: constants.borders.tableRow,
                borderLeft: '0',
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
            border: constants.borders.transparent,
            borderRight: '0',
          },
          ':last-of-type': {
            border: constants.borders.transparent,
            borderLeft: '0',
          },
        },
      }}>
      <thead>
        <tr>
          {columns.map(column => {
            return (
              <th key={column}>
                <div css={{ display: 'flex' }}>{column}</div>
              </th>
            )
          })}
        </tr>
      </thead>
      <tbody>
        {rows.map(
          ({
            id,
            state,
            name,
            username,
            assetCounts,
            priority,
            timeCreated,
            timeStarted,
            timeUpdated,
            taskCounts,
          }) => {
            return (
              <tr
                key={id}
                css={{
                  backgroundColor: colors.structure.lead,
                  '&:nth-of-type(2n)': {
                    backgroundColor: colors.structure.mattGrey,
                  },
                }}>
                <td>
                  <Status jobStatus={state} />
                </td>
                <td>{name}</td>
                <td>{username}</td>
                <td>{priority}</td>
                <td>{formatFullDate({ timestamp: timeCreated })}</td>
                <td>
                  {taskCounts.tasksFailure > 0 && (
                    <div
                      css={{
                        display: 'flex',
                        justifyContent: 'center',
                        color: colors.signal.warning.base,
                        fontWeight: typography.weight.bold,
                        fontSize: typography.size.kilo,
                        lineHeight: typography.height.kilo,
                        padding: spacing.base,
                        borderRadius: BORDER_RADIUS,
                        backgroundColor: colors.structure.coal,
                      }}>
                      {taskCounts.tasksFailure}
                    </div>
                  )}
                </td>
                <td>
                  {assetCounts.assetErrorCount > 0 && (
                    <div
                      css={{
                        display: 'flex',
                        justifyContent: 'center',
                        color: colors.signal.warning.base,
                        fontWeight: typography.weight.bold,
                        fontSize: typography.size.kilo,
                        lineHeight: typography.height.kilo,
                        padding: spacing.base,
                        borderRadius: BORDER_RADIUS,
                        backgroundColor: colors.structure.coal,
                      }}>
                      {assetCounts.assetErrorCount}
                    </div>
                  )}
                </td>
                <td>
                  {Object.values(assetCounts).reduce(
                    (total, count) => total + count,
                  )}
                </td>
                <td>
                  <ProgressBar
                    state={state}
                    taskCounts={taskCounts}
                    timeStarted={timeStarted}
                    timeUpdated={timeUpdated}
                  />
                </td>
              </tr>
            )
          },
        )}
      </tbody>
    </table>
  )
}

Table.propTypes = {
  columns: PropTypes.arrayOf(PropTypes.string).isRequired,
  rows: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      state: PropTypes.string,
      name: PropTypes.string,
      username: PropTypes.string,
      assetCounts: PropTypes.shape({
        assetCreatedCount: PropTypes.number,
        assetReplacedCount: PropTypes.number,
        assetWarningCount: PropTypes.number,
        assetErrorCount: PropTypes.number,
      }),
      priority: PropTypes.number,
      timeCreated: PropTypes.number,
      timeStarted: PropTypes.number,
      timeUpdated: PropTypes.number,
      taskCounts: PropTypes.shape({
        tasksFailure: PropTypes.number,
        tasksSkipped: PropTypes.number,
        tasksSuccess: PropTypes.number,
        tasksRunning: PropTypes.number,
        tasksPending: PropTypes.number,
      }).isRequired,
    }),
  ).isRequired,
}

export default Table
