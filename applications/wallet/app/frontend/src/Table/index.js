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
        boxShadow: constants.boxShadows.dark,
        th: {
          fontWeight: typography.weight.extraLight,
          color: colors.grey2,
          backgroundColor: colors.grey1,
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
          backgroundColor: colors.grey4,
          ':hover': {
            backgroundColor: colors.grey1,
            td: {
              border: constants.borders.default,
              borderLeft: '0',
              borderRight: '0',
              '&:first-of-type': {
                border: constants.borders.default,
                borderRight: '0',
              },
              '&:last-of-type': {
                border: constants.borders.default,
                borderLeft: '0',
              },
            },
          },
        },
        td: {
          whiteSpace: 'nowrap',
          fontWeight: typography.weight.extraLight,
          color: colors.grey2,
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
            taskCounts: {
              tasksFailure,
              tasksWaiting,
              tasksRunning,
              tasksSuccess,
            },
            assetCounts: { assetErrorCount },
            priority,
            timeCreated,
          }) => {
            return (
              <tr
                key={id}
                css={{
                  backgroundColor: colors.grey4,
                  '&:nth-of-type(2n)': {
                    backgroundColor: colors.grey3,
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
                  {tasksFailure > 0 && (
                    <div style={{ color: colors.error }}>{tasksFailure}</div>
                  )}
                </td>
                <td>
                  {assetErrorCount > 0 && (
                    <div style={{ color: colors.error }}>{assetErrorCount}</div>
                  )}
                </td>
                <td>numAsets</td>
                <td>
                  <ProgressBar
                    status={{
                      state,
                      username,
                      failed: tasksFailure,
                      pending: tasksWaiting,
                      running: tasksRunning,
                      succeeded: tasksSuccess,
                    }}
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
