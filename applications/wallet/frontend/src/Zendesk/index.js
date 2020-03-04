/* istanbul ignore file */
/* eslint-disable react/no-danger */
import Head from 'next/head'

import { colors } from '../Styles'

const SETTINGS = {
  webWidget: {
    color: {
      theme: colors.key.one,
      launcher: colors.key.one,
      launcherText: colors.structure.white,
      button: colors.key.one,
      resultLists: colors.key.one,
      header: colors.key.one,
      articleLinks: colors.key.one,
    },
    contactForm: {
      suppress: true,
      title: { '*': 'Contact Support' },
    },
    chat: { suppress: true },
    helpCenter: { suppress: true },
    talk: { suppress: true },
    answerBot: { suppress: true },
  },
}

const Zendesk = () => {
  return (
    <Head>
      <script
        id="ze-snippet"
        src="https://static.zdassets.com/ekr/snippet.js?key=267df0ed-b126-414e-8b9e-876d4af568fe"
        async
        defer
      />
      <script
        type="text/javascript"
        dangerouslySetInnerHTML={{
          __html: `window.zESettings = ${JSON.stringify(SETTINGS)}`,
        }}
      />
    </Head>
  )
}

export default Zendesk
