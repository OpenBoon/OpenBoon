import PropTypes from 'prop-types'
import { colors, constants, spacing, typography } from '../Styles'
import { formatFullDate } from '../Date/helpers'
import Status from '../Status'
import ProgressBar from '../ProgressBar'

const Table = ({ columns, rows }) => {
  return (
    <table
      css={{
        width: '100%',
        borderSpacing: 0,
        boxShadow: constants.boxShadows.table,
        th: {
          fontSize: '15px',
          fontWeight: typography.weight.medium,
          lineHeight: typography.height.mega,
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
          whiteSpace: 'nowrap',
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
            paused,
            state,
            name,
            createdUser: { username },
            taskCounts,
            assetCounts: { assetErrorCount },
            priority,
            timeCreated,
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
                  <Status jobStatus={paused ? 'Paused' : state} />
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
                        lineHeight: typography.height.mega,
                        padding: spacing.base,
                        borderRadius: 32,
                        backgroundColor: colors.structure.coal,
                      }}>
                      {taskCounts.tasksFailure}
                    </div>
                  )}
                </td>
                <td>
                  {assetErrorCount > 0 && (
                    <div
                      css={{
                        display: 'flex',
                        justifyContent: 'center',
                        color: colors.signal.warning.base,
                        fontWeight: typography.weight.bold,
                        fontSize: typography.size.kilo,
                        lineHeight: typography.height.mega,
                        padding: spacing.base,
                        borderRadius: 32,
                        backgroundColor: colors.structure.coal,
                      }}>
                      {assetErrorCount}
                    </div>
                  )}
                </td>
                <td>numAssets</td>
                <td>
                  <ProgressBar state={state} taskCounts={taskCounts} />
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
      paused: PropTypes.bool,
      state: PropTypes.string,
      name: PropTypes.string,
      createdUser: PropTypes.shape({
        username: PropTypes.string,
      }),
      taskCounts: PropTypes.shape({
        tasksFailure: PropTypes.number,
        tasksWaiting: PropTypes.number,
        tasksRunning: PropTypes.number,
        tasksSuccess: PropTypes.number,
      }),
      assetCounts: PropTypes.shape({
        assetErrorCount: PropTypes.number,
      }),
      priority: PropTypes.number,
      timeCreated: PropTypes.number,
    }),
  ).isRequired,
}

export default Table
