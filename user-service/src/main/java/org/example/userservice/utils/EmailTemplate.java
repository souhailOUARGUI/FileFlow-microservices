package org.example.userservice.utils;

public class EmailTemplate {
    
    public static String getVerificationCodeTemplate(String code) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Code de vérification FileFlow</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #f4f4f4;">
                    <tr>
                        <td style="padding: 40px 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">FileFlow</h1>
                                        <p style="margin: 10px 0 0 0; color: rgba(255,255,255,0.9); font-size: 14px;">Votre espace de stockage sécurisé</p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="margin: 0 0 20px 0; color: #333333; font-size: 22px; font-weight: 600;">Vérification de votre email</h2>
                                        <p style="margin: 0 0 30px 0; color: #666666; font-size: 16px; line-height: 1.6;">
                                            Bonjour,<br><br>
                                            Vous avez demandé un code de vérification pour votre compte FileFlow. Utilisez le code ci-dessous pour compléter votre inscription :
                                        </p>
                                        
                                        <!-- Code Box -->
                                        <div style="text-align: center; margin: 30px 0;">
                                            <div style="display: inline-block; padding: 20px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 8px;">
                                                <span style="font-size: 32px; font-weight: 700; color: #ffffff; letter-spacing: 8px;">%s</span>
                                            </div>
                                        </div>
                                        
                                        <p style="margin: 30px 0 0 0; color: #999999; font-size: 14px; line-height: 1.6;">
                                            <strong>Ce code expire dans 10 minutes.</strong><br><br>
                                            Si vous n'avez pas demandé ce code, vous pouvez ignorer cet email en toute sécurité.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 30px 40px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; border-top: 1px solid #eee;">
                                        <p style="margin: 0; color: #999999; font-size: 12px; text-align: center;">
                                            © 2024 FileFlow. Tous droits réservés.<br>
                                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(code);
    }

    public static String getPasswordResetTemplate(String resetLink) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Réinitialisation de mot de passe FileFlow</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #f4f4f4;">
                    <tr>
                        <td style="padding: 40px 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">FileFlow</h1>
                                        <p style="margin: 10px 0 0 0; color: rgba(255,255,255,0.9); font-size: 14px;">Votre espace de stockage sécurisé</p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="margin: 0 0 20px 0; color: #333333; font-size: 22px; font-weight: 600;">Réinitialisation de votre mot de passe</h2>
                                        <p style="margin: 0 0 30px 0; color: #666666; font-size: 16px; line-height: 1.6;">
                                            Bonjour,<br><br>
                                            Vous avez demandé la réinitialisation de votre mot de passe FileFlow. Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe :
                                        </p>
                                        
                                        <!-- Button -->
                                        <div style="text-align: center; margin: 30px 0;">
                                            <a href="%s" style="display: inline-block; padding: 16px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 600; border-radius: 8px;">
                                                Réinitialiser mon mot de passe
                                            </a>
                                        </div>
                                        
                                        <p style="margin: 30px 0 0 0; color: #999999; font-size: 14px; line-height: 1.6;">
                                            <strong>Ce lien expire dans 1 heure.</strong><br><br>
                                            Si vous n'avez pas demandé cette réinitialisation, vous pouvez ignorer cet email en toute sécurité. Votre mot de passe ne sera pas modifié.
                                        </p>
                                        
                                        <p style="margin: 20px 0 0 0; color: #999999; font-size: 12px; line-height: 1.6;">
                                            Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :<br>
                                            <a href="%s" style="color: #667eea; word-break: break-all;">%s</a>
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 30px 40px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; border-top: 1px solid #eee;">
                                        <p style="margin: 0; color: #999999; font-size: 12px; text-align: center;">
                                            © 2024 FileFlow. Tous droits réservés.<br>
                                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(resetLink, resetLink, resetLink);
    }
}
